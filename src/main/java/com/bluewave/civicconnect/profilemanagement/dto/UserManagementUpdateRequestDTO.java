package com.bluewave.civicconnect.profilemanagement.dto;

import com.bluewave.civicconnect.utils.constants.AccountStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserManagementUpdateRequestDTO {

    private String statusReason;

    @NotNull(message = "Account status is required")
    private AccountStatus accountStatus;
}