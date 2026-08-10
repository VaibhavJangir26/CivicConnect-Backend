package com.bluewave.civicconnect.category;


import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDeleteRequestDTO {

    @NotBlank(message = "id is required to delete")
    private String id;

}
