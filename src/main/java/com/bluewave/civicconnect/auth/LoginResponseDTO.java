package com.bluewave.civicconnect.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseDTO {

    private String message;
    private String accessToken;
    private String refreshToken;
    private Set<String> roles;
}
