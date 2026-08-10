package com.bluewave.civicconnect.users;

import com.bluewave.civicconnect.utils.common.SecurityUtils;
import com.bluewave.civicconnect.utils.constants.AccountStatus;
import com.bluewave.civicconnect.utils.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepo userRepo;
    private final SecurityUtils securityUtils; // Injected SecurityUtils

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Users user = userRepo.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

        // Account Status Checks
        if (user.getAccountStatus() == AccountStatus.SUSPENDED) {
            throw new DisabledException("Account is suspended. Please contact support.");
        }

        if (user.getAccountStatus() == AccountStatus.DELETED) {
            throw new DisabledException("Account no longer exists.");
        }

        // Return our custom UserDetails record
        return new CustomMyDetailsService(user);
    }
}