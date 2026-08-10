package com.bluewave.civicconnect.profile;
import com.bluewave.civicconnect.complains.Complains;
import com.bluewave.civicconnect.users.Users;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"users", "complains"})
@ToString(exclude = {"users","complains"})
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String fullName;

    @Column(unique = true)
    private String mobileNo;

    @Embedded
    private Address address;

    private LocalDate dob;

    private String imageUrl;

    private String imagePublicId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",unique = true,nullable = false)
    private Users users;


    @OneToMany(mappedBy = "profile",cascade = CascadeType.ALL,orphanRemoval = true)
    @Builder.Default
    private List<Complains> complains=new ArrayList<>();



}
