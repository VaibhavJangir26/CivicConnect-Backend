package com.bluewave.civicconnect.auth;

import com.bluewave.civicconnect.otp.VerifyOtpRequestDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/check-availability")
    public ResponseEntity<Map<String, Boolean>> checkAvailability(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email) {
        return ResponseEntity.ok(authService.checkAvailability(username, email));
    }

    // 2. Full Form Submission -> Validates DB -> Sends OTP
    @PostMapping("/signup-initiate")
    public ResponseEntity<Map<String, String>> initiateSignup(@Valid @RequestBody SignupRequestDTO dto) {
        String message = authService.initiateSignup(dto);
        return ResponseEntity.ok(Map.of("message", message));
    }

    // 3. User Enters 6-Digit OTP -> Creates User -> Returns Access/Refresh Token
    @PostMapping("/verify-and-register")
    public ResponseEntity<LoginResponseDTO> verifyAndRegister(@Valid @RequestBody VerifyOtpRequestDTO dto) {
        LoginResponseDTO response = authService.verifyAndRegister(dto);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        return ResponseEntity.ok(authService.login(dto));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<LoginResponseDTO> refreshToken(@Valid @RequestBody RefreshTokenRequestDTO dto) {
        return ResponseEntity.ok(authService.refreshToken(dto));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@Valid @RequestBody RefreshTokenRequestDTO dto) {
        authService.logout(dto);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}