package com.bluewave.civicconnect.category;

import com.bluewave.civicconnect.complains.Complains;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@EqualsAndHashCode(exclude = {"complains"})
@ToString(exclude = {"complains"})
public class Categories {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String categoryName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoryTypes categoryTypes;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "category",orphanRemoval = true)
    private List<Complains> complains=new ArrayList<>();


}
