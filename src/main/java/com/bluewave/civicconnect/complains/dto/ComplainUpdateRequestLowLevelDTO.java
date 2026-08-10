package com.bluewave.civicconnect.complains.dto;

import com.bluewave.civicconnect.complains.constatns.ComplainStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ComplainUpdateRequestLowLevelDTO {

    @NotBlank(message = "Complain ID is required")
    private String complainId;

    @NotNull(message = "Complain status is required")
    private ComplainStatus complainStatus;
}