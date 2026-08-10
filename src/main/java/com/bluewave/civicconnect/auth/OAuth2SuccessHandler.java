package com.bluewave.civicconnect.auth;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;

    @Value("${app.frontend.oauth-redirect:http://127.0.0.1:5500/oauth-success.html}")
    private String frontendRedirectUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = oauthToken.getPrincipal();
        String provider = oauthToken.getAuthorizedClientRegistrationId(); // "Google" or "GitHub"

        String email = extractEmail(oAuth2User, provider);
        String name = extractName(oAuth2User, provider);
        String username = email.split("@")[0] + "_" + provider;

        // Centralized OAuth processing in AuthService
        LoginResponseDTO loginResponse = authService.handleOAuth2Login(email, name, username);

        // Safe redirect using UriComponentsBuilder
        String targetUrl = UriComponentsBuilder.fromUriString(frontendRedirectUrl)
                .queryParam("accessToken", loginResponse.getAccessToken())
                .queryParam("refreshToken", loginResponse.getRefreshToken())
                .build().toUriString();

        response.sendRedirect(targetUrl);
    }

    private String extractEmail(OAuth2User oAuth2User, String provider) {
        String email = oAuth2User.getAttribute("email");
        if (email != null && !email.isBlank()) {
            return email;
        }

        // Provider fallback handling
        if ("github".equalsIgnoreCase(provider)) {
            String login = oAuth2User.getAttribute("login");
            if (login != null) {
                return login + "@users.noreply.github.com";
            }
        }

        throw new IllegalArgumentException("Unable to retrieve valid email address from provider: " + provider);
    }

    private String extractName(OAuth2User oAuth2User, String provider) {
        String name = oAuth2User.getAttribute("name");
        if (name != null && !name.isBlank()) {
            return name;
        }

        String login = oAuth2User.getAttribute("login");
        return login != null ? login : provider + "_user";
    }
}