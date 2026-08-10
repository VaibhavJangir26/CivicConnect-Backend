package com.bluewave.civicconnect.profile;

import com.bluewave.civicconnect.utils.common.CommonApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profile/me")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping()
    public ResponseEntity<CommonApiResponse<ProfileResponseDTO>> getMyProfile() {
        return ResponseEntity.ok(profileService.getMyProfile());
    }

    @PutMapping()
    public ResponseEntity<CommonApiResponse<ProfileResponseDTO>> updateProfile(
            @Valid @RequestBody UpdateProfileRequestDTO request) {
        return ResponseEntity.ok(profileService.updateProfile(request));
    }


    @DeleteMapping()
    @PreAuthorize("hasAnyRole('CITIZEN')")
    public ResponseEntity<CommonApiResponse<String>> deleteAccount() {
        return ResponseEntity.ok(profileService.deleteAccount());
    }
}