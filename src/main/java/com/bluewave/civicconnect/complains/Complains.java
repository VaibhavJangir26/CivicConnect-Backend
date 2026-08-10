package com.bluewave.civicconnect.complains;

import com.bluewave.civicconnect.category.Categories;
import com.bluewave.civicconnect.complains.constatns.ComplainPriority;
import com.bluewave.civicconnect.complains.constatns.ComplainStatus;
import com.bluewave.civicconnect.profile.Address;
import com.bluewave.civicconnect.profile.Profile;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "complaints")
@EqualsAndHashCode(exclude = {"profile", "assignedOfficer", "category"})
@ToString(exclude = {"profile", "assignedOfficer", "category"})
public class Complains {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 2000)
    private String message;

    @ElementCollection
    @CollectionTable(name = "complain_image_urls", joinColumns = @JoinColumn(name = "complain_id"))
    private List<String> imageUrls = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "complain_public_image_urls", joinColumns = @JoinColumn(name = "complain_id"))
    private List<String> publicImageId = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "complain_proof_image_urls", joinColumns = @JoinColumn(name = "complain_id"))
    private List<String> proofImageUrls = new ArrayList<>();

    @Embedded
    private Address address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplainPriority complainPriority = ComplainPriority.LOW;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplainStatus complainStatus = ComplainStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Categories category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "officer_id")
    private Profile assignedOfficer;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}