package com.bluewave.civicconnect.complains.dto;

import com.bluewave.civicconnect.category.CategoryResponseDTO;
import com.bluewave.civicconnect.complains.constatns.ComplainPriority;
import com.bluewave.civicconnect.complains.constatns.ComplainStatus;
import com.bluewave.civicconnect.profile.Address;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ComplainResponseDTO {

    private String id;
    private String message;
    private List<String> imageUrls;
    private List<String> publicImageId;
    private List<String> proofImageUrls;
    private Address address;
    private ComplainPriority complainPriority;
    private ComplainStatus complainStatus;
    private CategoryResponseDTO category;
    private String citizenName;
    private String assignedOfficerName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}