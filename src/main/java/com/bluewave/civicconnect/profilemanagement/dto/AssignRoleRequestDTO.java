package com.bluewave.civicconnect.profilemanagement.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AssignRoleRequestDTO {
    @NotBlank(message = "Role name is required")
    private String roleName;
}
