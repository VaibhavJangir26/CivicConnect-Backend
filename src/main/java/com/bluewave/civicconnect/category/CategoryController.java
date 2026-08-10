package com.bluewave.civicconnect.category;

import com.bluewave.civicconnect.utils.common.CommonApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/category")
@EnableMethodSecurity
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @PreAuthorize("hasAnyRole('CITIZEN','OFFICER','MANAGER','SUPER_ADMIN')")
    public ResponseEntity<CommonApiResponse<List<CategoryResponseDTO>>> getAllCategory() {
        return ResponseEntity.ok(categoryService.getAllCategory());
    }

    @GetMapping("/category-types")
    @PreAuthorize("hasAnyRole('CITIZEN', 'OFFICER', 'MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<CommonApiResponse<List<CategoryTypes>>> getAllCategoryTypes() {
        return ResponseEntity.ok(categoryService.getAllCategoryTypes());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<CommonApiResponse<CategoryResponseDTO>> createCategory(@Valid @RequestBody CategoryRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createCategory(dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<CommonApiResponse<String>> deleteCategoryById(@PathVariable String id) {
        return ResponseEntity.ok(categoryService.deleteCategoryById(id));
    }
}