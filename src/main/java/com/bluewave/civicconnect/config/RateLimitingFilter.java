package com.bluewave.civicconnect.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    // Store per-identifier active buckets: Key = "CATEGORY:USER:user_123" OR "CATEGORY:IP:192.168.1.1"
    private final Map<String, Bucket> activeBuckets = new ConcurrentHashMap<>();

    // Map storing path prefixes and their corresponding Bucket Creators
    private final Map<String, Supplier<Bucket>> routeRules = new LinkedHashMap<>();

    @PostConstruct
    public void initRouteRules() {
        // 1. AUTH / LOGIN / OTP: Strict limits (Unauthenticated -> IP based)
        routeRules.put("/api/v1/auth/signup-initiate",   () -> createBucket(3, 1, Duration.ofSeconds(30)));
        routeRules.put("/api/v1/auth/verify-and-register", () -> createBucket(3, 1, Duration.ofSeconds(30)));
        routeRules.put("/api/v1/auth/login",             () -> createBucket(5, 2, Duration.ofSeconds(10)));
        routeRules.put("/api/v1/auth/refresh-token",     () -> createBucket(10, 5, Duration.ofSeconds(10)));

        // 2. COMPLAINTS: Medium (Authenticated -> User based)
        routeRules.put("/api/v1/complains",              () -> createBucket(10, 5, Duration.ofSeconds(10)));

        // 3. PROFILE / CATEGORIES: High throughput (Authenticated -> User based)
        routeRules.put("/api/v1/profile",                () -> createBucket(30, 15, Duration.ofSeconds(5)));
        routeRules.put("/api/v1/categories",             () -> createBucket(30, 15, Duration.ofSeconds(5)));
    }

    private Bucket createBucket(long capacity, long refillTokens, Duration refillPeriod) {
        Bandwidth limit = Bandwidth.classic(capacity, Refill.greedy(refillTokens, refillPeriod));
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket createDefaultBucket() {
        return createBucket(20, 10, Duration.ofSeconds(1));
    }

    private Bucket resolveBucket(HttpServletRequest request) {
        String path = request.getRequestURI();
        String clientIdentifier = getClientIdentifier(request);

        // Find matching prefix rule or fall back to default
        Map.Entry<String, Supplier<Bucket>> matchedRule = routeRules.entrySet().stream()
                .filter(entry -> path.startsWith(entry.getKey()))
                .findFirst()
                .orElse(null);

        String category = (matchedRule != null) ? matchedRule.getKey() : "DEFAULT";
        Supplier<Bucket> bucketSupplier = (matchedRule != null) ? matchedRule.getValue() : this::createDefaultBucket;

        // Composite key ensures User-level or IP-level bucket isolation
        return activeBuckets.computeIfAbsent(category + ":" + clientIdentifier, k -> bucketSupplier.get());
    }

    /**
     * Extracts User Identity from JWT Token if available.
     * Falls back to Client IP address if request is unauthenticated.
     */
    private String getClientIdentifier(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            if (!token.isEmpty()) {
                String tokenHash = hashToken(token);
                return "USER:" + tokenHash; // Uniquely identifies the authenticated user
            }
        }

        // Fallback to IP address for unauthenticated requests
        return "IP:" + getClientIP(request);
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Fast SHA-256 hash of the JWT token to keep bucket map keys compact & memory-efficient.
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            // Fallback to raw token hashcode in rare cases
            return String.valueOf(token.hashCode());
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Bucket bucket = resolveBucket(request);

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
        } else {
            long waitForRefillInSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value()); // 429
            response.setContentType("application/json");
            response.setHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefillInSeconds));

            response.getWriter().write(
                    String.format("{\"error\": \"Too Many Requests\", \"message\": \"Rate limit exceeded for path %s. Try again in %d seconds.\"}",
                            request.getRequestURI(), waitForRefillInSeconds)
            );
        }
    }
}