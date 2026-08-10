package com.bluewave.civicconnect.users;

import com.bluewave.civicconnect.utils.constants.AccountStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

/**
 * Roles Entity - Represents application roles/permissions
 *
 * CRITICAL FIXES:
 * 1. @EqualsAndHashCode(exclude = "users") - Prevents StackOverflowError
 *    When hashCode() is called, it WON'T traverse the users Set
 * 2. @ToString(exclude = "users") - Prevents toString() infinite loops
 * 3. fetch = FetchType.EAGER - Load users collection eagerly (safer for most use cases)
 *
 * This breaks the circular reference chain:
 *   Users.hashCode() → excluded roles
 *   Roles.hashCode() → excluded users
 *   No infinite loop! ✅
 */
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(exclude = "users")  // ✅ FIX: Exclude users to prevent circular hashCode()
@ToString(exclude = "users")            // ✅ FIX: Exclude users to prevent circular toString()
public class Roles {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String roleName;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false)
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    /**
     * Bi-directional relationship with Users
     * This is the OWNING side (defined by @JoinTable)
     *
     * Changed to EAGER because:
     * - Small dataset (only a few roles)
     * - Roles are frequently accessed
     * - Avoids lazy initialization issues
     * - DataInitializer is @Transactional so no session issues
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "users_roles",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "users_id")
    )
    private Set<Users> users = new HashSet<>();

    @PrePersist
    public void prePersist() {
        if (this.accountStatus == null) {
            this.accountStatus = AccountStatus.ACTIVE;
        }
    }
}