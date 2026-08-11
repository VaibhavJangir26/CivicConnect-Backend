package com.bluewave.civicconnect.category;

import com.bluewave.civicconnect.utils.common.CommonApiResponse;
import com.bluewave.civicconnect.utils.exceptions.ResourceConflictException;
import com.bluewave.civicconnect.utils.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepo categoryRepo;

    private final String CategoryCachePrefix="Category:";

    @Cacheable(key ="'all'",value ="categoriesType" )
    public CommonApiResponse<List<CategoryTypes>> getAllCategoryTypes() {
        return CommonApiResponse.<List<CategoryTypes>>builder()
                .data(Arrays.asList(CategoryTypes.values()))
                .message("category types fetched successfully")
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Cacheable(key ="'all'",value ="categories" )
    public CommonApiResponse<List<CategoryResponseDTO>> getAllCategory() {
        List<CategoryResponseDTO> categories = categoryRepo.findAll().stream()
                .map(this::mapToDTO)
                .toList();

        return CommonApiResponse.<List<CategoryResponseDTO>>builder()
                .data(categories)
                .message("categories fetched successfully")
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @CacheEvict(key = "categories",allEntries = true)
    public CommonApiResponse<CategoryResponseDTO> createCategory(CategoryRequestDTO dto) {
        if (categoryRepo.findByCategoryName(dto.getCategoryName().trim()).isPresent()) {
            throw new ResourceConflictException("Category name already exists");
        }

        Categories category = new Categories();
        category.setCategoryName(dto.getCategoryName().trim());
        category.setCategoryTypes(dto.getCategoryTypes());

        Categories savedCategory = categoryRepo.save(category);

        return CommonApiResponse.<CategoryResponseDTO>builder()
                .message("Category created successfully")
                .data(mapToDTO(savedCategory))
                .statusCode(HttpStatus.CREATED.value())
                .success(true)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @CacheEvict(value = "categories", allEntries = true)
    public CommonApiResponse<String> deleteCategoryById(String id) {
        if (!categoryRepo.existsById(id)) {
            throw new ResourceNotFoundException("Category not found with id " +id);
        }
        categoryRepo.deleteById(id);

        return CommonApiResponse.<String>builder()
                .message("category deleted successfully")
                .data(null)
                .statusCode(HttpStatus.OK.value())
                .success(true)
                .timestamp(LocalDateTime.now())
                .build();
    }

    private CategoryResponseDTO mapToDTO(Categories category) {
        return CategoryResponseDTO.builder()
                .id(category.getId())
                .categoryName(category.getCategoryName())
                .categoryTypes(category.getCategoryTypes())
                .createdAt(category.getCreatedAt())
                .build();
    }
}