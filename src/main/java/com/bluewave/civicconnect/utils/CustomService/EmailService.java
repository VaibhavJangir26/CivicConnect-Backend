package com.bluewave.civicconnect.utils.CustomService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Service
public class EmailService {

    @Value("${google.client.id}")
    private String clientId;

    @Value("${google.client.secret}")
    private String clientSecret;

    @Value("${google.refresh.token}")
    private String refreshToken;

    @Value("${google.sender.email}")
    private String senderEmail;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();


    private String getAccessToken() throws Exception {
        String requestBody = String.format(
                "client_id=%s&client_secret=%s&refresh_token=%s&grant_type=refresh_token",
                clientId, clientSecret, refreshToken
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://oauth2.googleapis.com/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode jsonNode = objectMapper.readTree(response.body());

        if (!jsonNode.has("access_token")) {
            throw new RuntimeException("Failed to obtain OAuth access token from Google: " + response.body());
        }

        return jsonNode.get("access_token").asText();
    }


    // sends transactional email otp to any user via gmail rest api
    public void sendEmail(String to, String subject, String body) {
        try {
            // 1. Get Access Token
            String accessToken = getAccessToken();

            // 2. Build Raw RFC 2822 MIME Email string
            String rawEmail = "From: " + senderEmail + "\n" +
                    "To: " + to + "\n" +
                    "Subject: " + subject + "\n" +
                    "Content-Type: text/html; charset=utf-8\n\n" +
                    "<html><body><p>" + body.replace("\n", "<br>") + "</p></body></html>";

            // 3. Base64URL encode the raw email
            String encodedEmail = Base64.getUrlEncoder().withoutPadding().encodeToString(rawEmail.getBytes(StandardCharsets.UTF_8));

            String jsonPayload = objectMapper.writeValueAsString(Map.of("raw", encodedEmail));

            // 4. Send POST request to Gmail REST API
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://gmail.googleapis.com/gmail/v1/users/me/messages/send"))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 300) {
                throw new RuntimeException("Gmail API failed with HTTP " + response.statusCode() + ": " + response.body());
            }

        } catch (Exception e) {
            throw new RuntimeException("Error sending email via Gmail API: " + e.getMessage(), e);
        }
    }
}