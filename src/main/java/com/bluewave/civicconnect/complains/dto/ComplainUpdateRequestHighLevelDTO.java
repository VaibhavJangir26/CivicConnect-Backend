package com.bluewave.civicconnect.complains.dto;

import com.bluewave.civicconnect.complains.constatns.ComplainPriority;
import com.bluewave.civicconnect.complains.constatns.ComplainStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ComplainUpdateRequestHighLevelDTO {

    @NotBlank(message = "Complain ID is required")
    private String complainId;

    private String categoryId;
    private ComplainStatus status;
    private ComplainPriority complainPriority;
    private String assignedOfficerProfileId;
}