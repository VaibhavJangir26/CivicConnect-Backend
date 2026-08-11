package com.bluewave.civicconnect.profilemanagement;

import com.bluewave.civicconnect.profile.Profile;
import com.bluewave.civicconnect.profile.ProfileRepo;
import com.bluewave.civicconnect.profile.ProfileResponseDTO;
import com.bluewave.civicconnect.profilemanagement.dto.AssignRoleRequestDTO;
import com.bluewave.civicconnect.profilemanagement.dto.UserManagementRequestDTO;
import com.bluewave.civicconnect.profilemanagement.dto.UserManagementResponseDTO;
import com.bluewave.civicconnect.profilemanagement.dto.UserManagementUpdateRequestDTO;
import com.bluewave.civicconnect.users.RoleRepo;
import com.bluewave.civicconnect.users.Roles;
import com.bluewave.civicconnect.users.UserRepo;
import com.bluewave.civicconnect.users.Users;
import com.bluewave.civicconnect.utils.common.CommonApiResponse;
import com.bluewave.civicconnect.utils.common.SecurityUtils;
import com.bluewave.civicconnect.utils.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProfileManagementService {

    private final ProfileManagementRepo profileManagementRepo;
    private final ProfileRepo profileRepo;
    private final SecurityUtils securityUtils;
    private final UserRepo userRepo;
    private final RoleRepo roleRepo;

    private static final String USER_MANAGEMENT_SINGLE = "user_management_single";
    private static final String USER_MANAGEMENT_LIST = "user_management_list";
    private static final String USER_PROFILES_LIST = "user_profiles_list";


    @Transactional
    @Caching(evict = {
            @CacheEvict(value = USER_MANAGEMENT_SINGLE, allEntries = true),
            @CacheEvict(value = USER_MANAGEMENT_LIST, allEntries = true),
            @CacheEvict(value = USER_PROFILES_LIST, allEntries = true)
    })
    public CommonApiResponse<UserManagementResponseDTO> registerStaffUser(UserManagementRequestDTO dto) {
        Profile targetProfile = profileRepo.findById(dto.getProfileId())
                .orElseThrow(() -> new ResourceNotFoundException("User with this profile ID not found"));

        String currentAdmin = securityUtils.getCurrentUserName();

        ProfileManagement profileManagement = new ProfileManagement();
        profileManagement.setAccountStatus(dto.getAccountStatus());
        profileManagement.setStatusReason(dto.getStatusReason());
        profileManagement.setModifiedBy(currentAdmin);
        profileManagement.setStatusChangedAt(LocalDateTime.now());
        profileManagement.setUsers(targetProfile.getUsers());

        ProfileManagement saved = profileManagementRepo.save(profileManagement);

        return CommonApiResponse.<UserManagementResponseDTO>builder()
                .statusCode(HttpStatus.CREATED.value())
                .message("Staff status registered successfully")
                .timestamp(LocalDateTime.now())
                .success(true)
                .data(mapToDto(saved))
                .build();
    }


    @Transactional(readOnly = true)
    @Cacheable(value = USER_MANAGEMENT_LIST, key = "@securityUtils.getCurrentUserName()")
    public CommonApiResponse<List<UserManagementResponseDTO>> getAllUsers() {
        Set<String> roles = securityUtils.getCurrentUserRoles();

        // 1. Backend RBAC Guard Clause
        if (!roles.contains("ROLE_SUPER_ADMIN") && !roles.contains("ROLE_MANAGER")) {
            throw new AccessDeniedException("Access denied. Insufficient permissions to view user management list.");
        }

        List<ProfileManagement> rawList = profileManagementRepo.findAll();

        // 2. Role-based Scope Filtering
        if (roles.contains("ROLE_MANAGER") && !roles.contains("ROLE_SUPER_ADMIN")) {
            rawList = rawList.stream()
                    .filter(pm -> pm.getUsers() != null && pm.getUsers().getRoles().stream()
                            .anyMatch(role -> "ROLE_OFFICER".equalsIgnoreCase(role.getRoleName())))
                    .toList();
        }

        List<UserManagementResponseDTO> dtoList = rawList.stream()
                .map(this::mapToDto)
                .toList();

        return CommonApiResponse.<List<UserManagementResponseDTO>>builder()
                .message("All user statuses fetched successfully")
                .timestamp(LocalDateTime.now())
                .success(true)
                .data(dtoList)
                .statusCode(HttpStatus.OK.value())
                .build();
    }


    @Transactional(readOnly = true)
    @Cacheable(value = USER_PROFILES_LIST, key = "@securityUtils.getCurrentUserName()")
    public CommonApiResponse<List<ProfileResponseDTO>> getAllProfiles() {
        Set<String> roles = securityUtils.getCurrentUserRoles();

        if (!roles.contains("ROLE_SUPER_ADMIN") && !roles.contains("ROLE_MANAGER")) {
            throw new AccessDeniedException("Access denied. Insufficient permissions to view profile listings.");
        }

        List<ProfileResponseDTO> list = profileRepo.findAll().stream()
                .map(profile -> {
                    Users user = profile.getUsers();
                    Set<String> userRoles = securityUtils.getUserRoles(user);
                    return ProfileResponseDTO.builder()
                            .id(profile.getId())
                            .fullName(profile.getFullName() != null ? profile.getFullName() : (user != null ? user.getFullName() : null))
                            .username(user != null ? user.getUsername() : null)
                            .email(user != null ? user.getEmail() : null)
                            .mobileNo(profile.getMobileNo())
                            .address(profile.getAddress())
                            .dob(profile.getDob())
                            .imageUrl(profile.getImageUrl())
                            .publicImageUrl(profile.getImagePublicId())
                            .roles(userRoles)
                            .accountStatus(user != null ? user.getAccountStatus() : null)
                            .createdAt(profile.getCreatedAt())
                            .updatedAt(profile.getUpdatedAt())
                            .build();
                }).toList();

        return CommonApiResponse.<List<ProfileResponseDTO>>builder()
                .message("All citizen profiles fetched successfully")
                .timestamp(LocalDateTime.now())
                .success(true)
                .data(list)
                .statusCode(HttpStatus.OK.value())
                .build();
    }


    @Transactional
    @Caching(evict = {
            @CacheEvict(value = USER_MANAGEMENT_SINGLE, allEntries = true),
            @CacheEvict(value = USER_MANAGEMENT_LIST, allEntries = true),
            @CacheEvict(value = USER_PROFILES_LIST, allEntries = true)
    })
    public CommonApiResponse<UserManagementResponseDTO> updateUserStatus(UserManagementUpdateRequestDTO dto, String id) {
        ProfileManagement pm = profileManagementRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User management ID not found"));

        Users targetUser = pm.getUsers();
        Set<String> adminRoles = securityUtils.getCurrentUserRoles();
        String currentAdmin = securityUtils.getCurrentUserName();

        if (adminRoles.contains("ROLE_MANAGER") && !adminRoles.contains("ROLE_SUPER_ADMIN")) {
            boolean isTargetOfficer = targetUser != null && targetUser.getRoles().stream()
                    .anyMatch(role -> "ROLE_OFFICER".equalsIgnoreCase(role.getRoleName()));

            if (!isTargetOfficer) {
                throw new AccessDeniedException("Managers are only permitted to update Officer accounts.");
            }
        }

        pm.setAccountStatus(dto.getAccountStatus());
        pm.setStatusReason(dto.getStatusReason());
        pm.setModifiedBy(currentAdmin);
        pm.setStatusChangedAt(LocalDateTime.now());

        // Sync status back to target user entity if needed
        if (targetUser != null && dto.getAccountStatus() != null) {
            targetUser.setAccountStatus(dto.getAccountStatus());
            userRepo.save(targetUser);
        }

        ProfileManagement updated = profileManagementRepo.save(pm);

        return CommonApiResponse.<UserManagementResponseDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("User status updated successfully")
                .timestamp(LocalDateTime.now())
                .success(true)
                .data(mapToDto(updated))
                .build();
    }


    @Transactional
    @Caching(evict = {
            @CacheEvict(value = USER_MANAGEMENT_SINGLE, allEntries = true),
            @CacheEvict(value = USER_MANAGEMENT_LIST, allEntries = true),
            @CacheEvict(value = USER_PROFILES_LIST, allEntries = true),
            @CacheEvict(value = "user_profile", allEntries = true)
    })
    public CommonApiResponse<String> assignRole(String profileId, AssignRoleRequestDTO dto) {
        Profile targetProfile = profileRepo.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        Users targetUser = targetProfile.getUsers();
        if (targetUser == null) {
            throw new ResourceNotFoundException("User associated with this profile not found");
        }

        Set<String> adminRoles = securityUtils.getCurrentUserRoles();

        String newRoleName = dto.getRoleName().trim().toUpperCase();
        if (!newRoleName.startsWith("ROLE_")) {
            newRoleName = "ROLE_" + newRoleName;
        }

        if (adminRoles.contains("ROLE_MANAGER") && !adminRoles.contains("ROLE_SUPER_ADMIN")) {
            if (!"ROLE_OFFICER".equals(newRoleName)) {
                throw new AccessDeniedException("Managers are only permitted to assign the OFFICER role.");
            }
        }

        final String finalRoleName = newRoleName;
        Roles newRole = roleRepo.findByRoleName(newRoleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + finalRoleName));

        for (Roles role : targetUser.getRoles()) {
            role.getUsers().remove(targetUser);
        }
        targetUser.getRoles().clear();

        targetUser.getRoles().add(newRole);
        newRole.getUsers().add(targetUser);

        userRepo.save(targetUser);

        return CommonApiResponse.<String>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Role assigned successfully to " + newRoleName)
                .timestamp(LocalDateTime.now())
                .success(true)
                .data(newRoleName)
                .build();
    }


    @Transactional
    @Caching(evict = {
            @CacheEvict(value = USER_MANAGEMENT_SINGLE, allEntries = true),
            @CacheEvict(value = USER_MANAGEMENT_LIST, allEntries = true),
            @CacheEvict(value = USER_PROFILES_LIST, allEntries = true)
    })
    public void deleteUser(String id) {
        if (!profileManagementRepo.existsById(id)) {
            throw new ResourceNotFoundException("User management ID not found");
        }
        profileManagementRepo.deleteById(id);
    }

    private UserManagementResponseDTO mapToDto(ProfileManagement profileManagement) {
        return UserManagementResponseDTO.builder()
                .id(profileManagement.getId())
                .createdAt(profileManagement.getCreatedAt())
                .updatedAt(profileManagement.getUpdatedAt())
                .statusChangedAt(profileManagement.getStatusChangedAt())
                .modifiedBy(profileManagement.getModifiedBy())
                .accountStatus(profileManagement.getAccountStatus())
                .statusReason(profileManagement.getStatusReason())
                .targetUserName(profileManagement.getUsers() != null ? profileManagement.getUsers().getFullName() : null)
                .build();
    }
}