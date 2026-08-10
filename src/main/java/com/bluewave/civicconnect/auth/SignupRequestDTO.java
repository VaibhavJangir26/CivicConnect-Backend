package com.bluewave.civicconnect.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SignupRequestDTO {

    @NotBlank(message = "full name is required")
    private String fullName;

    @NotBlank(message = "email is required")
    @Email
    private String email;

    @NotNull(message = "Username is required")
    private String username;

    @NotNull(message = "password is required")
    @Size(min = 6,message = "min 6 digit password required")
    private String password;

}
