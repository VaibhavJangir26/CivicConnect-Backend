package com.bluewave.civicconnect.category;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryResponseDTO implements Serializable {

    @Serial
    private static final long serialVersionUID=1L;

    private String id;
    private String categoryName;
    private CategoryTypes categoryTypes;
    private LocalDateTime createdAt;
}
