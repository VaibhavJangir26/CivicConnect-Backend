package com.bluewave.civicconnect.complains.dto;

import com.bluewave.civicconnect.complains.constatns.ComplainPriority;
import com.bluewave.civicconnect.profile.Address;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComplainRequestDTO {

    @NotBlank(message = "Complain message is required")
    private String message;

    @NotNull(message = "Address is required")
    @Valid
    private Address address;

    @NotNull(message = "Complain priority is required")
    private ComplainPriority complainPriority;

    @NotBlank(message = "Category ID is required")
    private String categoryId;
}