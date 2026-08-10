package com.bluewave.civicconnect.otp;

import com.bluewave.civicconnect.auth.SignupRequestDTO;
import com.bluewave.civicconnect.utils.CustomService.EmailService;
import com.bluewave.civicconnect.utils.exceptions.BadRequestException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final EmailService emailService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    // Redis Keys Prefixes
    private static final String OTP_PREFIX = "OTP:";
    private static final String ATTEMPTS_PREFIX = "OTP_ATTEMPTS:";
    private static final String COOLDOWN_PREFIX = "OTP_COOLDOWN:";
    private static final String PAYLOAD_PREFIX = "SIGNUP_PAYLOAD:";

    // Constants
    private static final long OTP_EXPIRATION_MINUTES = 5;
    private static final long COOLDOWN_SECONDS = 60;
    private static final int MAX_ATTEMPTS = 3;

    /**
     * Generates a secure 6-digit OTP.
     */
    private String generateOTP() {
        SecureRandom secureRandom = new SecureRandom();
        int num = secureRandom.nextInt(1_000_000);
        return String.format("%06d", num);
    }

    /**
     * Step 1: Generate OTP, cache registration payload in Redis, and send Email.
     */
    public void generateAndSendOtp(String email, SignupRequestDTO signupDto) {
        String normalizedEmail = email.toLowerCase().trim();
        String cooldownKey = COOLDOWN_PREFIX + normalizedEmail;

        // Rate Limiter: Prevent spamming the email service
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            throw new BadRequestException("Please wait 60 seconds before requesting another OTP");
        }

        String otp = generateOTP();
        String otpKey = OTP_PREFIX + normalizedEmail;
        String attemptsKey = ATTEMPTS_PREFIX + normalizedEmail;
        String payloadKey = PAYLOAD_PREFIX + normalizedEmail;

        try {
            // Serialize DTO to JSON
            String signupJson = objectMapper.writeValueAsString(signupDto);

            // Store OTP and payload in Redis with TTL (Time To Live)
            redisTemplate.opsForValue().set(otpKey, otp, Duration.ofMinutes(OTP_EXPIRATION_MINUTES));
            redisTemplate.opsForValue().set(attemptsKey, "0", Duration.ofMinutes(OTP_EXPIRATION_MINUTES));
            redisTemplate.opsForValue().set(payloadKey, signupJson, Duration.ofMinutes(OTP_EXPIRATION_MINUTES));

            // Set Cooldown lock
            redisTemplate.opsForValue().set(cooldownKey, "true", Duration.ofSeconds(COOLDOWN_SECONDS));

            // Dispatch Email
            String emailBody = String.format(
                    "Your FinEdgeBank verification code is: %s.\nThis code will expire in %d minutes.",
                    otp, OTP_EXPIRATION_MINUTES
            );
            emailService.sendEmail(normalizedEmail, "FinEdgeBank - Verification Code", emailBody);

            log.info("OTP generated and sent successfully to {}", normalizedEmail);

        } catch (Exception e) {
            // Cleanup on serialization or email failure
            clearOtpSession(normalizedEmail);
            log.error("Failed to send OTP to {}: {}", normalizedEmail, e.getMessage());
            throw new BadRequestException("Failed to send OTP email. Please try again.");
        }
    }

    /**
     * Step 2: Validate OTP and return payload (DOES NOT DELETE KEYS YET).
     */
    public SignupRequestDTO validateOtpAndGetPayload(String email, String inputOtp) {
        String normalizedEmail = email.toLowerCase().trim();
        String otpKey = OTP_PREFIX + normalizedEmail;
        String attemptsKey = ATTEMPTS_PREFIX + normalizedEmail;
        String payloadKey = PAYLOAD_PREFIX + normalizedEmail;

        String cachedOtp = redisTemplate.opsForValue().get(otpKey);

        if (cachedOtp == null) {
            throw new BadRequestException("OTP has expired or was not requested. Please submit registration again.");
        }

        // Clean cached OTP (Removes extra quotes if Redis serializer appended them)
        cachedOtp = cachedOtp.replace("\"", "").trim();

        // Increment attempt tracker
        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
        if (attempts != null && attempts > MAX_ATTEMPTS) {
            clearOtpSession(normalizedEmail); // Wipe session if brute-forced
            throw new BadRequestException("Maximum OTP verification attempts exceeded. Please restart registration.");
        }

        // Validate Input
        if (!cachedOtp.equals(inputOtp.trim())) {
            long remaining = MAX_ATTEMPTS - (attempts != null ? attempts : 0);
            throw new BadRequestException("Invalid OTP. Remaining attempts: " + Math.max(0, remaining));
        }

        // Fetch user payload
        String payloadJson = redisTemplate.opsForValue().get(payloadKey);
        if (payloadJson == null) {
            throw new BadRequestException("Registration session expired. Please submit registration again.");
        }

        try {
            // Note: We DO NOT delete Redis keys here anymore! We wait until DB saves successfully.
            return objectMapper.readValue(payloadJson, SignupRequestDTO.class);
        } catch (Exception e) {
            throw new BadRequestException("Failed to process registration session data.");
        }
    }

    /**
     * Step 3: Wipes OTP data from Redis ONLY AFTER successful DB commit.
     */
    public void clearOtpSession(String email) {
        String normalizedEmail = email.toLowerCase().trim();
        redisTemplate.delete(OTP_PREFIX + normalizedEmail);
        redisTemplate.delete(ATTEMPTS_PREFIX + normalizedEmail);
        redisTemplate.delete(PAYLOAD_PREFIX + normalizedEmail);
        redisTemplate.delete(COOLDOWN_PREFIX + normalizedEmail);
        log.info("Cleared Redis OTP session for {}", normalizedEmail);
    }
}