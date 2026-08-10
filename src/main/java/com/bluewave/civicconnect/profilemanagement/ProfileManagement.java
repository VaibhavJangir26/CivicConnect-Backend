package com.bluewave.civicconnect.profilemanagement;

import com.bluewave.civicconnect.users.Users;
import com.bluewave.civicconnect.utils.constants.AccountStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@ToString(exclude = "users")
@EqualsAndHashCode(exclude = "users")
public class ProfileManagement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(length = 500)
    private String statusReason;

    private String modifiedBy;

    @Enumerated(EnumType.STRING)
    private AccountStatus accountStatus=AccountStatus.ACTIVE;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime statusChangedAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToOne(fetch =FetchType.LAZY)
    @JoinColumn(name = "user_id",unique = true,nullable = false)
    private Users users;

}
