package com.bluewave.civicconnect.auth;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequestDTO {

    @NotNull(message = "Username is required")
    private String username;

    @NotNull(message = "password is required")
    @Size(min = 6,message = "min 6 digit password required")
    private String password;

}
