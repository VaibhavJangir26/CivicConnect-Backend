package com.bluewave.civicconnect.config;

import com.bluewave.civicconnect.auth.OAuth2SuccessHandler;
import com.bluewave.civicconnect.utils.common.MyAuthenticationCheckPoint;
import com.bluewave.civicconnect.utils.common.MyAccessPermissionHandler;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${jwt.secret}")
    private String secretKey;

    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final CorsConfigurationSource corsConfigurationSource;
    private final MyAuthenticationCheckPoint myAuthenticationCheckPoint;
    private final MyAccessPermissionHandler myAccessPermissionHandler;

    public SecurityConfig(@Lazy OAuth2SuccessHandler oAuth2SuccessHandler, CorsConfigurationSource corsConfigurationSource, MyAuthenticationCheckPoint myAuthenticationCheckPoint, MyAccessPermissionHandler myAccessPermissionHandler) {
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
        this.corsConfigurationSource = corsConfigurationSource;
        this.myAuthenticationCheckPoint = myAuthenticationCheckPoint;
        this.myAccessPermissionHandler = myAccessPermissionHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**", "/error").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "MANAGER", "SUPER_ADMIN")
                        .requestMatchers("/api/v1/manager/**").hasAnyRole("ADMIN", "MANAGER", "SUPER_ADMIN")
                        .requestMatchers("/api/v1/user/**").hasAnyRole("USER", "CITIZEN", "ADMIN", "MANAGER", "SUPER_ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex->ex.authenticationEntryPoint(myAuthenticationCheckPoint).accessDeniedHandler(myAccessPermissionHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2SuccessHandler)
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(jwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                );

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        return NimbusJwtDecoder.withSecretKey(key).build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<String> roles = jwt.getClaimAsStringList("roles");
            if (roles == null || roles.isEmpty()) {
                return Collections.emptyList();
            }

            return roles.stream()
                    // Ensures authority starts with ROLE_ for hasRole() matches
                    .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        });

        return converter;
    }
}