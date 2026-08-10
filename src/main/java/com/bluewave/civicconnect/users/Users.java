package com.bluewave.civicconnect.users;

import com.bluewave.civicconnect.profile.Profile;
import com.bluewave.civicconnect.profilemanagement.ProfileManagement;
import com.bluewave.civicconnect.utils.constants.AccountStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Users Entity - Represents application users
 *
 * CRITICAL FIXES:
 * 1. @EqualsAndHashCode(exclude = {"roles", "profile"}) - Prevents StackOverflowError
 *    When hashCode() is called, it WON'T traverse the bidirectional relationship
 * 2. @ToString(exclude = {"roles", "profile"}) - Prevents toString() infinite loops
 * 3. Roles collection fetch = LAZY (but safe because @Transactional in DataInitializer)
 */
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"roles", "profile"})
@ToString(exclude = {"roles", "profile"})
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true)
    private String email;

    private String fullName;
    private String password;

    @Column(nullable = false)
    private boolean enabled = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false)
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    /**
     * Bi-directional relationship with Roles
     * mappedBy = "users" means Roles.users is the owning side
     * LAZY is safe here because DataInitializer is @Transactional
     */
    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "users")
    private Set<Roles> roles = new HashSet<>();

    /**
     * One-to-One relationship with Profile
     * Excluded from hashCode/toString to prevent infinite loops
     */
    @OneToOne(mappedBy = "users", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Profile profile;

    @OneToOne(mappedBy = "users",cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    private ProfileManagement profileManagement;
}