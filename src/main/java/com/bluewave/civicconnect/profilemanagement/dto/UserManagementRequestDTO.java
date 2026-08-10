package com.bluewave.civicconnect.profilemanagement.dto;

import com.bluewave.civicconnect.utils.constants.AccountStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserManagementRequestDTO {

    @NotBlank(message = "Status reason is required")
    private String statusReason;

    @NotBlank(message = "Profile ID is required")
    private String profileId;

    @NotNull(message = "Account status is required")
    private AccountStatus accountStatus;
}