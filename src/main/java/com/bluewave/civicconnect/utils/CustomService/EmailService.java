package com.bluewave.civicconnect.utils.CustomService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

@Service
public class EmailService {

    @Value("${resend.api.key}")
    private String resendApiKey;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void sendEmail(String to, String subject, String body) {
        try {
            Map<String, Object> emailRequest = Map.of(
                "from", "CivicConnect <onboarding@resend.dev>",
                "to", new String[]{to},
                "subject", subject,
                "text", body
            );

            String requestBody = objectMapper.writeValueAsString(emailRequest);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.resend.com/emails"))
                .header("Authorization", "Bearer " + resendApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 300) {
                throw new RuntimeException("Failed to send email via Resend API: " + response.body());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error sending email: " + e.getMessage(), e);
        }
    }
}
