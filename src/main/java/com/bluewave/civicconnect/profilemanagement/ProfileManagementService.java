package com.bluewave.civicconnect.profilemanagement;

import com.bluewave.civicconnect.profile.Profile;
import com.bluewave.civicconnect.profile.ProfileRepo;
import com.bluewave.civicconnect.profilemanagement.dto.UserManagementRequestDTO;
import com.bluewave.civicconnect.profilemanagement.dto.UserManagementResponseDTO;
import com.bluewave.civicconnect.profilemanagement.dto.UserManagementUpdateRequestDTO;
import com.bluewave.civicconnect.users.Users;
import com.bluewave.civicconnect.utils.common.CommonApiResponse;
import com.bluewave.civicconnect.utils.common.SecurityUtils;
import com.bluewave.civicconnect.utils.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.bluewave.civicconnect.profile.ProfileResponseDTO;
import com.bluewave.civicconnect.users.RoleRepo;
import com.bluewave.civicconnect.users.Roles;
import com.bluewave.civicconnect.users.UserRepo;
import com.bluewave.civicconnect.profilemanagement.dto.AssignRoleRequestDTO;

@Service
@RequiredArgsConstructor
public class ProfileManagementService {

    private final ProfileManagementRepo profileManagementRepo;
    private final ProfileRepo profileRepo;
    private final SecurityUtils securityUtils;
    private final UserRepo userRepo;
    private final RoleRepo roleRepo;

    public CommonApiResponse<UserManagementResponseDTO> registerStaffUser(UserManagementRequestDTO dto) {
        Profile targetProfile = profileRepo.findById(dto.getProfileId())
                .orElseThrow(() -> new ResourceNotFoundException("User with this profile ID not found"));

        // Get the logged-in admin performing this action
        String currentAdmin = securityUtils.getCurrentUserName();

        ProfileManagement profileManagement = new ProfileManagement();
        profileManagement.setAccountStatus(dto.getAccountStatus());
        profileManagement.setStatusReason(dto.getStatusReason());
        profileManagement.setModifiedBy(currentAdmin); // ✅ Safely sets the string username
        profileManagement.setStatusChangedAt(LocalDateTime.now());
        profileManagement.setUsers(targetProfile.getUsers()); // ✅ Link it to the actual User!

        ProfileManagement saved = profileManagementRepo.save(profileManagement);

        return CommonApiResponse.<UserManagementResponseDTO>builder()
                .statusCode(HttpStatus.CREATED.value())
                .message("Staff status registered successfully")
                .timestamp(LocalDateTime.now())
                .success(true)
                .data(mapToDto(saved))
                .build();
    }

    public CommonApiResponse<List<UserManagementResponseDTO>> getAllUsers() {
        // Fetch all profiles. Frontend can filter by status.
        // Note: For strict security, you could filter here so Managers only get Officers.
        List<UserManagementResponseDTO> list = profileManagementRepo.findAll()
                .stream().map(this::mapToDto).toList();

        return CommonApiResponse.<List<UserManagementResponseDTO>>builder()
                .message("All user statuses fetched successfully")
                .timestamp(LocalDateTime.now())
                .success(true)
                .data(list)
                .statusCode(HttpStatus.OK.value())
                .build();
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public CommonApiResponse<List<ProfileResponseDTO>> getAllProfiles() {
        List<ProfileResponseDTO> list = profileRepo.findAll().stream()
                .map(profile -> {
                    Users user = profile.getUsers();
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
                }).toList();

        return CommonApiResponse.<List<ProfileResponseDTO>>builder()
                .message("All citizen profiles fetched successfully")
                .timestamp(LocalDateTime.now())
                .success(true)
                .data(list)
                .statusCode(HttpStatus.OK.value())
                .build();
    }

    public CommonApiResponse<UserManagementResponseDTO> updateUserStatus(UserManagementUpdateRequestDTO dto, String id) {
        // ✅ Load existing record instead of creating a new one
        ProfileManagement pm = profileManagementRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User management ID not found"));

        Users targetUser = pm.getUsers();
        Set<String> adminRoles = securityUtils.getCurrentUserRoles();
        String currentAdmin = securityUtils.getCurrentUserName();

        // ✅ Scope Rule: If the admin is a MANAGER (and not a SUPER_ADMIN), they can ONLY update OFFICERS
        if (adminRoles.contains("ROLE_MANAGER") && !adminRoles.contains("ROLE_SUPER_ADMIN")) {
            boolean isTargetOfficer = targetUser.getRoles().stream()
                    .anyMatch(role -> role.getRoleName().equals("ROLE_OFFICER"));

            if (!isTargetOfficer) {
                throw new AccessDeniedException("Managers are only permitted to update Officer accounts.");
            }
        }

        pm.setAccountStatus(dto.getAccountStatus());
        pm.setStatusReason(dto.getStatusReason());
        pm.setModifiedBy(currentAdmin);
        pm.setStatusChangedAt(LocalDateTime.now());

        ProfileManagement updated = profileManagementRepo.save(pm);

        return CommonApiResponse.<UserManagementResponseDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("User status updated successfully")
                .timestamp(LocalDateTime.now())
                .success(true)
                .data(mapToDto(updated))
                .build();
    }

    @org.springframework.transaction.annotation.Transactional
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

        // Hierarchy check
        if (adminRoles.contains("ROLE_MANAGER") && !adminRoles.contains("ROLE_SUPER_ADMIN")) {
            if (!newRoleName.equals("ROLE_OFFICER")) {
                throw new AccessDeniedException("Managers are only permitted to assign the OFFICER role.");
            }
        }

        final String finalRoleName = newRoleName;
        Roles newRole = roleRepo.findByRoleName(newRoleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + finalRoleName));

        // Manage bi-directional relationship
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