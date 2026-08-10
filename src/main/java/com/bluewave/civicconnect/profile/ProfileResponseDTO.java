package com.bluewave.civicconnect.profile;

import com.bluewave.civicconnect.utils.constants.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileResponseDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String id;
    private String fullName;
    private String username;
    private String email;
    private String mobileNo;
    private Address address;
    private LocalDate dob;
    private String imageUrl;
    private String publicImageUrl;
    private Set<String> roles;
    private AccountStatus accountStatus; // Account status read from Users entity
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}