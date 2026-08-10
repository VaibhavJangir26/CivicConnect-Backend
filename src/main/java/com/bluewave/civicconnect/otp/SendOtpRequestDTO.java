package com.bluewave.civicconnect.otp;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendOtpRequestDTO {

    @Email
    @NotNull(message = "email is required")
    private String email;

}
