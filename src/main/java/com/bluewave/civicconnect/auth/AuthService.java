package com.bluewave.civicconnect.auth;

import com.bluewave.civicconnect.otp.OtpService;
import com.bluewave.civicconnect.otp.VerifyOtpRequestDTO;
import com.bluewave.civicconnect.profile.Profile;
import com.bluewave.civicconnect.users.RoleRepo;
import com.bluewave.civicconnect.users.Roles;
import com.bluewave.civicconnect.users.UserRepo;
import com.bluewave.civicconnect.users.Users;
import com.bluewave.civicconnect.utils.CustomService.EmailService;
import com.bluewave.civicconnect.utils.CustomService.JWTService;
import com.bluewave.civicconnect.utils.constants.AppRole;
import com.bluewave.civicconnect.utils.exceptions.ResourceConflictException;
import com.bluewave.civicconnect.utils.exceptions.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepo userRepo;
    private final RoleRepo roleRepo;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JWTService jwtService;
    private final EmailService emailService;
    private final OtpService otpService;

    /**
     * 1. Check availability of Username or Email during live form validation.
     * This is useful for frontend validation before the user submits the form.
     */
    public Map<String, Boolean> checkAvailability(String username, String email) {
        Map<String, Boolean> response = new HashMap<>();
        if (username != null && !username.isBlank()) {
            response.put("usernameExists", userRepo.findByUsername(username.trim()).isPresent());
        }
        if (email != null && !email.isBlank()) {
            response.put("emailExists", userRepo.findByEmail(email.toLowerCase().trim()).isPresent());
        }
        return response;
    }

    /**
     * 2. Initiate Registration: Verifies unique constraints before dispatching verification OTP.
     */
    @Transactional
    public String initiateSignup(SignupRequestDTO dto) {
        String normalizedEmail = dto.getEmail().toLowerCase().trim();
        String normalizedUsername = dto.getUsername().trim();

        // Unique Constraint Guard: Fail fast if data already exists in DB
        if (userRepo.findByUsername(normalizedUsername).isPresent()) {
            throw new ResourceConflictException("Username '" + normalizedUsername + "' is already taken");
        }
        if (userRepo.findByEmail(normalizedEmail).isPresent()) {
            throw new ResourceConflictException("Email '" + normalizedEmail + "' is already registered");
        }

        // Generate OTP, cache signup details in Redis, and send email via OtpService
        otpService.generateAndSendOtp(normalizedEmail, dto);

        return "Verification OTP sent successfully to " + normalizedEmail;
    }

    /**
     * 3. Verify OTP, Persist User + Shell Profile to PostgreSQL, and Return JWT Credentials.
     */
    @Transactional
    public LoginResponseDTO verifyAndRegister(VerifyOtpRequestDTO dto) {
        // 1. Validate OTP (Retrieves payload, but DOES NOT delete from Redis yet to prevent rollback bugs)
        SignupRequestDTO signupData = otpService.validateOtpAndGetPayload(dto.getEmail(), dto.getOtp());

        String normalizedEmail = signupData.getEmail().toLowerCase().trim();
        String normalizedUsername = signupData.getUsername().trim();

        // Final safety check to prevent race conditions during DB insert
        if (userRepo.findByEmail(normalizedEmail).isPresent() || userRepo.findByUsername(normalizedUsername).isPresent()) {
            throw new ResourceConflictException("Account already exists.");
        }

        // Fetch the default role required for all new standard users
        Roles defaultRole = roleRepo.findByRoleName(AppRole.ROLE_CITIZEN.name())
                .orElseThrow(() -> new ResourceNotFoundException("CRITICAL ERROR: Default role ROLE_CITIZEN not found in database. Seed your roles table."));

        // 2. Build Core User Entity
        Users user = new Users();
        user.setEmail(normalizedEmail);
        user.setFullName(signupData.getFullName().trim());
        user.setUsername(normalizedUsername);
        user.setPassword(passwordEncoder.encode(signupData.getPassword()));
        user.setEnabled(true);

        // Maintain bi-directional relationship for Roles
        user.getRoles().add(defaultRole);
        defaultRole.getUsers().add(user);

        // 3. Build linked Profile Shell for Onboarding
        // We leave mobileNo and address empty so frontend can trigger "Complete Profile" flow later
        Profile shellProfile = Profile.builder()
                .fullName(signupData.getFullName().trim())
                .users(user) // Link the foreign key
                .build();

        user.setProfile(shellProfile);

        // 4. Force SQL Execution
        // saveAndFlush triggers the actual INSERT queries immediately.
        // If DB constraints fail (e.g., missing NOT NULL fields), transaction aborts here cleanly.
        userRepo.saveAndFlush(user);

        // 5. SUCCESS! DB write passed. Safely wipe the OTP session from Redis.
        otpService.clearOtpSession(normalizedEmail);

        // Prepare User Authorities for JWT Token mapping
        Set<String> roles = user.getRoles().stream()
                .map(Roles::getRoleName)
                .collect(Collectors.toSet());

        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();

        // 6. Dispatch Welcome Email asynchronously/safely
        try {
            String messageBody = String.format(
                    "Hello %s,\n\nWelcome to FinEdgeBank! Your account [%s] was created successfully.",
                    user.getFullName(), user.getUsername()
            );
            emailService.sendEmail(user.getEmail(), "Welcome to FinEdgeBank!", messageBody);
        } catch (Exception e) {
            log.warn("Welcome email failed for {}, but registration succeeded.", user.getEmail());
        }

        // 7. Generate JWT Tokens for Auto-Login immediately after sign up
        Authentication auth = new UsernamePasswordAuthenticationToken(user.getUsername(), null, authorities);
        String accessToken = jwtService.generateAccessToken(auth);
        String refreshToken = jwtService.generateRefreshToken(user.getUsername());

        return LoginResponseDTO.builder()
                .message("Registration successful! Welcome to FinEdgeBank.")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .roles(roles)
                .build();
    }

    /**
     * 4. Standard Password Login
     */
    public LoginResponseDTO login(LoginRequestDTO dto) {
        String identifier = dto.getUsername().trim();

        // Spring Security handles the password hashing comparison and status checks automatically
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(identifier, dto.getPassword())
        );

        // Extract roles to inject into JWT claims and JSON response
        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        String accessToken = jwtService.generateAccessToken(authentication);
        String refreshToken = jwtService.generateRefreshToken(authentication.getName());

        return LoginResponseDTO.builder()
                .message("Login successful")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .roles(roles)
                .build();
    }

    /**
     * 5. OAuth2 Social Login Handler
     * Automatically registers users if they login via Google/Github for the first time.
     */
    @Transactional
    public LoginResponseDTO handleOAuth2Login(String email, String name, String defaultUsername) {
        String normalizedEmail = email.toLowerCase().trim();

        // Default role for new social accounts
        Roles defaultRole = roleRepo.findByRoleName(AppRole.ROLE_CITIZEN.name())
                .orElseThrow(() -> new ResourceNotFoundException("CRITICAL ERROR: Default role ROLE_CITIZEN not found."));

        // Retrieve existing user by email, or create a brand new one if it doesn't exist
        Users user = userRepo.findByEmail(normalizedEmail).orElseGet(() -> {

            Users newUser = new Users();
            newUser.setEmail(normalizedEmail);
            newUser.setFullName(name != null ? name.trim() : "Social User");

            // Guarantee a unique username by appending UUID if the default OAuth name is taken
            String finalUsername = defaultUsername.trim();
            if (userRepo.findByUsername(finalUsername).isPresent()) {
                finalUsername = finalUsername + "_" + UUID.randomUUID().toString().substring(0, 5);
            }

            newUser.setUsername(finalUsername);
            // OAuth users don't use passwords, so we encode a random UUID to prevent raw password login
            newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            newUser.setEnabled(true);

            // Assign default role
            newUser.getRoles().add(defaultRole);
            defaultRole.getUsers().add(newUser);

            // Create Linked Shell Profile so OAuth users can also update profile details later
            Profile shellProfile = Profile.builder()
                    .fullName(newUser.getFullName())
                    .users(newUser)
                    .build();

            newUser.setProfile(shellProfile);

            // Execute immediate DB insert to catch any constraint issues immediately
            return userRepo.saveAndFlush(newUser);
        });

        // Extract authorities for the new or existing user
        Set<String> roles = user.getRoles().stream()
                .map(Roles::getRoleName)
                .collect(Collectors.toSet());

        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();

        // Manually build authentication token for JWT generation since we bypassed AuthenticationManager
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user.getUsername(), null, authorities);

        String accessToken = jwtService.generateAccessToken(auth);
        String refreshToken = jwtService.generateRefreshToken(user.getUsername());

        return LoginResponseDTO.builder()
                .message("OAuth2 Login successful")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .roles(roles)
                .build();
    }

    /**
     * 6. Refresh Expired Access Token
     * Validates the refresh token and issues a new short-lived access token.
     */
    public LoginResponseDTO refreshToken(RefreshTokenRequestDTO dto) {
        String refreshToken = dto.getRefreshToken();

        if (!jwtService.isTokenValid(refreshToken)) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        String username = jwtService.extractUsername(refreshToken);
        Users user = userRepo.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for token refresh: " + username));

        // Always query roles fresh from DB in case admin changed user roles since last login
        Set<String> roles = user.getRoles().stream()
                .map(Roles::getRoleName)
                .collect(Collectors.toSet());

        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();

        // Reissue the token
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user.getUsername(), null, authorities);
        String newAccessToken = jwtService.generateAccessToken(auth);

        return LoginResponseDTO.builder()
                .message("Token refreshed successfully")
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .roles(roles)
                .build();
    }

    /**
     * 7. Logout Handler
     * Note: In JWT stateless architecture, true logout requires adding the token to a Redis Blacklist.
     * This currently handles frontend cleanup log signaling.
     */
    @Transactional
    public void logout(RefreshTokenRequestDTO dto) {
        String refreshToken = dto.getRefreshToken();

        if (!jwtService.isTokenValid(refreshToken)) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        String username = jwtService.extractUsername(refreshToken);

        // (Optional: Implement Redis Token Blacklisting here if strict revocation is required)
        log.info("User [{}] successfully logged out.", username);
    }
}