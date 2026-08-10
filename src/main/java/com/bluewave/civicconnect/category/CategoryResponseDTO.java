package com.bluewave.civicconnect.category;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryResponseDTO {

    private String id;
    private String categoryName;
    private CategoryTypes categoryTypes;
    private LocalDateTime createdAt;
}
