package com.bluewave.civicconnect.profile;

import com.bluewave.civicconnect.users.UserRepo;
import com.bluewave.civicconnect.users.Users;
import com.bluewave.civicconnect.utils.common.CommonApiResponse;
import com.bluewave.civicconnect.utils.common.SecurityUtils;
import com.bluewave.civicconnect.utils.constants.AccountStatus;
import com.bluewave.civicconnect.utils.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepo profileRepo;
    private final UserRepo userRepo;
    private final SecurityUtils securityUtils;

    private static final String PROFILE_CACHE = "user_profile";


    @Transactional(readOnly = true)
    @Cacheable(value = PROFILE_CACHE, key = "'PROFILE:' + @securityUtils.getCurrentUserName()")
    public CommonApiResponse<ProfileResponseDTO> getMyProfile() {

        Users currentUser = securityUtils.getCurrentUser();

        Profile profile = profileRepo.findByUsers_Username(currentUser.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user: " + currentUser.getUsername()));

        return CommonApiResponse.<ProfileResponseDTO>builder()
                .data(mapToDTO(profile, currentUser))
                .message("Profile fetched successfully")
                .success(true)
                .statusCode(200)
                .timestamp(LocalDateTime.now())
                .build();
    }


    @Transactional
    @CacheEvict(value = PROFILE_CACHE, key = "'PROFILE:' + @securityUtils.getCurrentUserName()")
    public CommonApiResponse<ProfileResponseDTO> updateProfile(UpdateProfileRequestDTO request) {

        Users currentUser = securityUtils.getCurrentUser();

        Profile profile = profileRepo.findByUsers_Username(currentUser.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user: " + currentUser.getUsername()));

        // Update profile fields if provided in request
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            profile.setFullName(request.getFullName());
        }
        if (request.getMobileNo() != null && !request.getMobileNo().isBlank()) {
            profile.setMobileNo(request.getMobileNo());
        }
        if (request.getDob() != null) {
            profile.setDob(request.getDob());
        }
        if (request.getAddress() != null) {
            profile.setAddress(request.getAddress());
        }

        Profile savedProfile = profileRepo.save(profile);

        return CommonApiResponse.<ProfileResponseDTO>builder()
                .data(mapToDTO(savedProfile, currentUser))
                .message("Profile updated successfully")
                .success(true)
                .statusCode(200)
                .timestamp(LocalDateTime.now())
                .build();
    }


    @Transactional
    @CacheEvict(value = PROFILE_CACHE, key = "'PROFILE:' + @securityUtils.getCurrentUserName()")
    public CommonApiResponse<String> deleteAccount() {

        Users currentUser = securityUtils.getCurrentUser();

        // Update status in Identity domain
        currentUser.setAccountStatus(AccountStatus.DELETED);
        userRepo.save(currentUser);

        return CommonApiResponse.<String>builder()
                .data("Account marked as DELETED successfully")
                .message("Account deleted successfully")
                .success(true)
                .statusCode(200)
                .timestamp(LocalDateTime.now())
                .build();
    }


    private ProfileResponseDTO mapToDTO(Profile profile, Users user) {
        Set<String> roles = securityUtils.getUserRoles(user);

        return ProfileResponseDTO.builder()
                .id(profile.getId())
                .fullName(profile.getFullName() != null ? profile.getFullName() : user.getFullName())
                .username(user.getUsername())
                .email(user.getEmail())
                .mobileNo(profile.getMobileNo())
                .address(profile.getAddress())
                .dob(profile.getDob())
                .imageUrl(profile.getImageUrl())
                .publicImageUrl(profile.getImagePublicId())
                .roles(roles)
                .accountStatus(user.getAccountStatus())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}