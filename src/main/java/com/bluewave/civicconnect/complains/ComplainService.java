package com.bluewave.civicconnect.complains;

import com.bluewave.civicconnect.category.Categories;
import com.bluewave.civicconnect.category.CategoryRepo;
import com.bluewave.civicconnect.category.CategoryResponseDTO;
import com.bluewave.civicconnect.complains.constatns.ComplainStatus;
import com.bluewave.civicconnect.complains.dto.*;
import com.bluewave.civicconnect.profile.Profile;
import com.bluewave.civicconnect.profile.ProfileRepo;
import com.bluewave.civicconnect.users.Users;
import com.bluewave.civicconnect.utils.CustomService.ImageUploadService;
import com.bluewave.civicconnect.utils.common.CommonApiResponse;
import com.bluewave.civicconnect.utils.common.SecurityUtils;
import com.bluewave.civicconnect.utils.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ComplainService {

    private final ComplainRepo complainRepo;
    private final CategoryRepo categoryRepo;
    private final ProfileRepo profileRepo;
    private final SecurityUtils securityUtils;
    private final ImageUploadService imageUploadService;
    private final ComplainSearchService complainSearchService;

    // citizen can create the complains
    @Caching(evict = {
            @CacheEvict(value = "complains_list", allEntries = true),
            @CacheEvict(value = "complains_search", allEntries = true),
            @CacheEvict(value = "complains_suggest", allEntries = true)
    })
    @Transactional
    public CommonApiResponse<ComplainResponseDTO> createComplain(ComplainRequestDTO dto, List<MultipartFile> images) {
        Categories category = categoryRepo.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category ID not found: " + dto.getCategoryId()));

        // Extract logged-in user profile safely via JWT context
        Profile citizenProfile = securityUtils.getCurrentUser().getProfile();

        Complains complain = new Complains();
        complain.setMessage(dto.getMessage());
        complain.setAddress(dto.getAddress());
        complain.setCategory(category);
        complain.setProfile(citizenProfile);
        complain.setComplainPriority(dto.getComplainPriority());
        complain.setComplainStatus(ComplainStatus.PENDING);

        // Upload images to Cloudinary if provided
        if (images != null && !images.isEmpty()) {
            for (MultipartFile file : images) {
                if (!file.isEmpty()) {
                    Map<String, Object> uploadResult = imageUploadService.uploadImage(file);
                    complain.getImageUrls().add((String) uploadResult.get("secure_url"));
                    complain.getPublicImageId().add((String) uploadResult.get("public_id"));
                }
            }
        }

        Complains savedComplain = complainRepo.save(complain);

        // Synchronize with Elasticsearch Index
        complainSearchService.indexComplain(savedComplain);

        return buildResponse("Complain created successfully", HttpStatus.CREATED, savedComplain);
    }

    // rbac for complains
    @Cacheable(value = "complains_list", key = "@securityUtils.getCurrentUserName() + '-' + (#status != null ? #status : 'ALL')")
    public CommonApiResponse<List<ComplainResponseDTO>> getComplains(ComplainStatus status) {
        Users currentUser = securityUtils.getCurrentUser();
        Set<String> roles = securityUtils.getCurrentUserRoles();
        List<Complains> complainsList;

        if (roles.contains("ROLE_SUPER_ADMIN") || roles.contains("ROLE_MANAGER")) {
            complainsList = (status != null) ?
                    complainRepo.findByComplainStatus(status) :
                    complainRepo.findAll();
        } else if (roles.contains("ROLE_OFFICER")) {
            complainsList = (status != null) ?
                    complainRepo.findByAssignedOfficerAndComplainStatus(currentUser.getProfile(), status) :
                    complainRepo.findByAssignedOfficer(currentUser.getProfile());
        } else {
            complainsList = (status != null) ?
                    complainRepo.findByProfileAndComplainStatus(currentUser.getProfile(), status) :
                    complainRepo.findByProfile(currentUser.getProfile());
        }

        List<ComplainResponseDTO> dtoList = complainsList.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        return CommonApiResponse.<List<ComplainResponseDTO>>builder()
                .message("Complaints fetched successfully")
                .data(dtoList)
                .statusCode(HttpStatus.OK.value())
                .success(true)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // any one can access
    @Cacheable(value = "complains_single", key = "#id")
    public CommonApiResponse<ComplainResponseDTO> getComplainDetails(String id) {
        Complains complain = complainRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complain ID not found: " + id));
        return buildResponse("Complain details fetched successfully", HttpStatus.OK, complain);
    }

    // officer change the status
    @Caching(evict = {
            @CacheEvict(value = "complains_single", key = "#dto.complainId"),
            @CacheEvict(value = "complains_list", allEntries = true),
            @CacheEvict(value = "complains_search", allEntries = true),
            @CacheEvict(value = "complains_suggest", allEntries = true)
    })
    @Transactional
    public CommonApiResponse<ComplainResponseDTO> updateComplainStatus(ComplainUpdateRequestLowLevelDTO dto, List<MultipartFile> proofImages) {
        Complains complain = complainRepo.findById(dto.getComplainId())
                .orElseThrow(() -> new ResourceNotFoundException("Complain ID not found: " + dto.getComplainId()));

        complain.setComplainStatus(dto.getComplainStatus());

        // Process proof images when completing work
        if (dto.getComplainStatus() == ComplainStatus.COMPLETED && proofImages != null && !proofImages.isEmpty()) {
            for (MultipartFile file : proofImages) {
                if (!file.isEmpty()) {
                    Map<String, Object> uploadResult = imageUploadService.uploadImage(file);
                    complain.getProofImageUrls().add((String) uploadResult.get("secure_url"));
                }
            }
        }

        Complains updatedComplain = complainRepo.save(complain);

        // Synchronize updated state to Elasticsearch
        complainSearchService.indexComplain(updatedComplain);

        return buildResponse("Complain status updated successfully", HttpStatus.OK, updatedComplain);
    }

    // manger can manage the complain
    @Caching(evict = {
            @CacheEvict(value = "complains_single", key = "#dto.complainId"),
            @CacheEvict(value = "complains_list", allEntries = true),
            @CacheEvict(value = "complains_search", allEntries = true),
            @CacheEvict(value = "complains_suggest", allEntries = true)
    })
    @Transactional
    public CommonApiResponse<ComplainResponseDTO> manageComplain(ComplainUpdateRequestHighLevelDTO dto) {
        Complains complain = complainRepo.findById(dto.getComplainId())
                .orElseThrow(() -> new ResourceNotFoundException("Complain ID not found: " + dto.getComplainId()));

        if (dto.getCategoryId() != null) {
            complain.setCategory(categoryRepo.findById(dto.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + dto.getCategoryId())));
        }

        if (dto.getStatus() != null) {
            complain.setComplainStatus(dto.getStatus());
        }

        if (dto.getComplainPriority() != null) {
            complain.setComplainPriority(dto.getComplainPriority());
        }

        if (dto.getAssignedOfficerProfileId() != null) {
            Profile officerProfile = profileRepo.findById(dto.getAssignedOfficerProfileId())
                    .orElseThrow(() -> new ResourceNotFoundException("Officer Profile not found: " + dto.getAssignedOfficerProfileId()));
            complain.setAssignedOfficer(officerProfile);
            complain.setComplainStatus(ComplainStatus.ASSIGNED);
        }

        Complains updatedComplain = complainRepo.save(complain);

        // Synchronize updated state to Elasticsearch
        complainSearchService.indexComplain(updatedComplain);

        return buildResponse("Complain managed successfully", HttpStatus.OK, updatedComplain);
    }

    // admin or superadmin can delete
    @Caching(evict = {
            @CacheEvict(value = "complains_single", key = "#id"),
            @CacheEvict(value = "complains_list", allEntries = true),
            @CacheEvict(value = "complains_search", allEntries = true),
            @CacheEvict(value = "complains_suggest", allEntries = true)
    })
    @Transactional
    public CommonApiResponse<String> deleteComplain(String id) {
        Complains complain = complainRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complain ID not found: " + id));

        // Delete images from Cloudinary
        if (complain.getPublicImageId() != null) {
            for (String publicId : complain.getPublicImageId()) {
                imageUploadService.deleteImage(publicId);
            }
        }

        complainRepo.delete(complain);

        // Remove from Elasticsearch Index
        complainSearchService.deleteComplainFromIndex(id);

        return CommonApiResponse.<String>builder()
                .message("Complain deleted successfully")
                .success(true)
                .statusCode(HttpStatus.NO_CONTENT.value())
                .timestamp(LocalDateTime.now())
                .build();
    }

    // Helper method to wrap response
    private CommonApiResponse<ComplainResponseDTO> buildResponse(String msg, HttpStatus status, Complains complain) {
        return CommonApiResponse.<ComplainResponseDTO>builder()
                .message(msg)
                .statusCode(status.value())
                .success(true)
                .data(mapToDTO(complain))
                .timestamp(LocalDateTime.now())
                .build();
    }

    // Entity to DTO Mapper
    private ComplainResponseDTO mapToDTO(Complains complain) {
        return ComplainResponseDTO.builder()
                .id(complain.getId())
                .message(complain.getMessage())
                .address(complain.getAddress())
                .citizenName(complain.getProfile() != null ? complain.getProfile().getFullName() : null)
                .assignedOfficerName(complain.getAssignedOfficer() != null ? complain.getAssignedOfficer().getFullName() : null)
                .complainPriority(complain.getComplainPriority())
                .complainStatus(complain.getComplainStatus())
                .imageUrls(complain.getImageUrls())
                .publicImageId(complain.getPublicImageId())
                .proofImageUrls(complain.getProofImageUrls())
                .category(CategoryResponseDTO.builder()
                        .id(complain.getCategory().getId())
                        .categoryName(complain.getCategory().getCategoryName())
                        .categoryTypes(complain.getCategory().getCategoryTypes())
                        .build())
                .createdAt(complain.getCreatedAt())
                .updatedAt(complain.getUpdatedAt())
                .build();
    }
}