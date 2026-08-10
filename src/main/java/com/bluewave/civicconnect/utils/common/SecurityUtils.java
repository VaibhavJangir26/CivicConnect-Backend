package com.bluewave.civicconnect.utils.common;

import com.bluewave.civicconnect.users.Roles;
import com.bluewave.civicconnect.users.UserRepo;
import com.bluewave.civicconnect.users.Users;
import com.bluewave.civicconnect.utils.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepo userRepo;

    /**
     * Extracts username from active SecurityContext
     */
    public String getCurrentUserName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new BadCredentialsException("No authenticated user found in security context");
        }
        return authentication.getName();
    }

    /**
     * Fetches current Users entity from database
     */
    public Users getCurrentUser() {
        String username = getCurrentUserName();
        return userRepo.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    /**
     * Reusable Helper: Extracts role names as a Set of Strings from a given Users entity
     */
    public Set<String> getUserRoles(Users user) {
        if (user == null || user.getRoles() == null) {
            return Collections.emptySet();
        }
        return user.getRoles().stream()
                .map(Roles::getRoleName)
                .collect(Collectors.toSet());
    }

    /**
     * Reusable Helper: Extracts role names as a Set of Strings directly for the logged-in user
     */
    public Set<String> getCurrentUserRoles() {
        return getUserRoles(getCurrentUser());
    }

    /**
     * Reusable Helper: Converts user roles to Spring Security GrantedAuthorities
     */
    public List<GrantedAuthority> getUserAuthorities(Users user) {
        return getUserRoles(user).stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    /**
     * Helper to check if the current logged-in user possesses a specific role
     */
    public boolean hasRole(String roleName) {
        return getCurrentUserRoles().contains(roleName);
    }
}