package com.bluewave.civicconnect.category;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequestDTO {

    @NotBlank(message = "category name is required")
    private String categoryName;

    @jakarta.validation.constraints.NotNull(message = "category type is required")
    private CategoryTypes categoryTypes;

}
