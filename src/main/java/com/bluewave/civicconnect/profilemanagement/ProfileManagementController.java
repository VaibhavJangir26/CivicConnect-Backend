package com.bluewave.civicconnect.profilemanagement;

import com.bluewave.civicconnect.profilemanagement.dto.UserManagementRequestDTO;
import com.bluewave.civicconnect.profilemanagement.dto.UserManagementResponseDTO;
import com.bluewave.civicconnect.profilemanagement.dto.UserManagementUpdateRequestDTO;
import com.bluewave.civicconnect.profilemanagement.dto.AssignRoleRequestDTO;
import com.bluewave.civicconnect.utils.common.CommonApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.bluewave.civicconnect.profile.ProfileResponseDTO;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class ProfileManagementController {

    private final ProfileManagementService profileManagementService;


    @PostMapping("/staff")
    @PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<CommonApiResponse<UserManagementResponseDTO>> registerStaffUser(
            @Valid @RequestBody UserManagementRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(profileManagementService.registerStaffUser(dto));
    }


    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<CommonApiResponse<List<UserManagementResponseDTO>>> getAllUsers() {
        return ResponseEntity.ok(profileManagementService.getAllUsers());
    }

    @GetMapping("/profiles")
    @PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<CommonApiResponse<List<ProfileResponseDTO>>> getAllProfiles() {
        return ResponseEntity.ok(profileManagementService.getAllProfiles());
    }


    @PatchMapping("/users/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<CommonApiResponse<UserManagementResponseDTO>> updateUserStatus(
            @Valid @RequestBody UserManagementUpdateRequestDTO dto,
            @PathVariable String id) {
        return ResponseEntity.ok(profileManagementService.updateUserStatus(dto, id));
    }

    @PatchMapping("/users/{id}/roles")
    @PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<CommonApiResponse<String>> assignRole(
            @Valid @RequestBody AssignRoleRequestDTO dto,
            @PathVariable String id) {
        return ResponseEntity.ok(profileManagementService.assignRole(id, dto));
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<CommonApiResponse<Void>> deleteUser(
            @PathVariable String id) {
        profileManagementService.deleteUser(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
    }
}