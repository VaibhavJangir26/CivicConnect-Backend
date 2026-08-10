package com.bluewave.civicconnect.config;


import com.bluewave.civicconnect.users.RoleRepo;
import com.bluewave.civicconnect.users.Roles;
import com.bluewave.civicconnect.users.UserRepo;
import com.bluewave.civicconnect.users.Users;
import com.bluewave.civicconnect.utils.constants.AppRole;
import com.bluewave.civicconnect.profile.Profile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DataInitializer - Seeds default roles and super admin account on application startup
 *
 * Key fixes:
 * 1. Added @Transactional to create Hibernate session
 * 2. Use getRoles().add() instead of setRoles(Set.of())
 * 3. Properly maintain bi-directional relationship
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final PasswordEncoder passwordEncoder;
    private final UserRepo userRepo;
    private final RoleRepo roleRepo;

    /**
     * Creates or retrieves an existing role
     * Safe to call within @Transactional context
     */
    private Roles getOrCreateRole(String roleName) {
        return roleRepo.findByRoleName(roleName).orElseGet(() -> {
            Roles roles = new Roles();
            roles.setRoleName(roleName);
            log.debug("Creating role: {}", roleName);
            return roleRepo.save(roles);
        });
    }

    /**
     * Initialize baseline system roles and Super Admin account
     *
     * ✅ FIX: Added @Transactional to provide Hibernate session
     * This allows safe access to lazy-loaded collections
     */
    @Override
    @Transactional
    public void run(@NotNull String... args) throws Exception {
        log.info("Initializing baseline system roles and Super Admin...");

        // Create/retrieve all default roles
        Roles superAdminRole = getOrCreateRole(AppRole.ROLE_SUPER_ADMIN.name());
        getOrCreateRole(AppRole.ROLE_MANAGER.name());
        getOrCreateRole(AppRole.ROLE_OFFICER.name());
        getOrCreateRole(AppRole.ROLE_CITIZEN.name());

        String superAdminEmail = "superadmin123@finedgebank.com";
        Users superAdminUser = userRepo.findByEmail(superAdminEmail).orElse(null);
        if (superAdminUser == null) {
            superAdminUser = new Users();
            superAdminUser.setUsername("superadmin123");
            superAdminUser.setFullName("Super Admin");
            superAdminUser.setEmail(superAdminEmail);
            superAdminUser.setPassword(passwordEncoder.encode("superAdmin@12345"));
            superAdminUser.setEnabled(true);

            // ✅ FIX #1: Use getRoles() getter to maintain JPA managed collection
            // DON'T use: superAdminUser.setRoles(Set.of(superAdminRole));
            // Because Set.of() creates an immutable collection that JPA can't track
            superAdminUser.getRoles().add(superAdminRole);

            // ✅ FIX #2: Now safe to access lazy collection because @Transactional
            // provides active Hibernate session
            superAdminRole.getUsers().add(superAdminUser);

            // Create linked Profile shell
            Profile profile = Profile.builder()
                    .fullName("Super Admin")
                    .users(superAdminUser)
                    .build();
            superAdminUser.setProfile(profile);

            // ✅ FIX #3: Save user (cascade will handle role relationship)
            userRepo.save(superAdminUser);

            log.info(">>> SUCCESS: Seeded initial Super Admin account -> {}", superAdminEmail);
        } else {
            // If the Super Admin user already exists but does not have a profile, create it.
            if (superAdminUser.getProfile() == null) {
                Profile profile = Profile.builder()
                        .fullName("Super Admin")
                        .users(superAdminUser)
                        .build();
                superAdminUser.setProfile(profile);
                userRepo.save(superAdminUser);
                log.info(">>> SUCCESS: Seeded missing Profile for existing Super Admin.");
            } else {
                log.info(">>> Super Admin account and profile already exist. Skipping initialization.");
            }
        }
    }
}