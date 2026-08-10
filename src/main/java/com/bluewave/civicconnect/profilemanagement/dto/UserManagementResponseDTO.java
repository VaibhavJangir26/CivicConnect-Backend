package com.bluewave.civicconnect.profilemanagement.dto;

import com.bluewave.civicconnect.utils.constants.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserManagementResponseDTO {
    private String id;
    private String statusReason;
    private String modifiedBy;
    private AccountStatus accountStatus;
    private String targetUserName;
    private LocalDateTime statusChangedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}