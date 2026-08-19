package com.bluewave.civicconnect.complains;

import com.bluewave.civicconnect.category.Categories;
import com.bluewave.civicconnect.category.CategoryTypes;
import com.bluewave.civicconnect.complains.constatns.ComplainPriority;
import com.bluewave.civicconnect.complains.constatns.ComplainStatus;
import com.bluewave.civicconnect.complains.dto.ComplainResponseDTO;
import com.bluewave.civicconnect.profile.Address;
import com.bluewave.civicconnect.profile.Profile;
import com.bluewave.civicconnect.users.Users;
import com.bluewave.civicconnect.utils.common.CommonApiResponse;
import com.bluewave.civicconnect.utils.common.PaginationResponse;
import com.bluewave.civicconnect.utils.common.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComplainServiceTest {

    @Mock
    private ComplainRepo complainRepo;

    @Mock
    private ComplainSearchService complainSearchService;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private ComplainService complainService;

    private Complains sampleComplain;
    private Profile sampleProfile;
    private Users sampleUser;

    @BeforeEach
    void setUp() {
        Categories sampleCategory = new Categories();
        sampleCategory.setId("cat-1");
        sampleCategory.setCategoryName("Roads");
        sampleCategory.setCategoryTypes(CategoryTypes.INFRASTRUCTURE_ROADS);
        sampleCategory.setComplains(new ArrayList<>());

        sampleProfile = new Profile();
        sampleProfile.setId("profile-1");
        sampleProfile.setFullName("John Doe");

        sampleUser = new Users();
        sampleUser.setId("user-1");
        sampleUser.setUsername("johndoe");
        sampleUser.setProfile(sampleProfile);

        Address address = new Address("Metropolis", "USA", "NY", "10001", "123 Main St");

        sampleComplain = new Complains();
        sampleComplain.setId("comp-1");
        sampleComplain.setMessage("Pothole on 5th Ave");
        sampleComplain.setAddress(address);
        sampleComplain.setCategory(sampleCategory);
        sampleComplain.setProfile(sampleProfile);
        sampleComplain.setComplainPriority(ComplainPriority.HIGH);
        sampleComplain.setComplainStatus(ComplainStatus.PENDING);
        sampleComplain.setCreatedAt(LocalDateTime.now());
        sampleComplain.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void testGetComplains_ForAdmin_ReturnsPaginatedResponse() {
        when(securityUtils.getCurrentUser()).thenReturn(sampleUser);
        when(securityUtils.getCurrentUserRoles()).thenReturn(Set.of("ROLE_MANAGER"));

        Page<Complains> complainPage = new PageImpl<>(List.of(sampleComplain));
        when(complainRepo.findAll(any(Pageable.class))).thenReturn(complainPage);

        CommonApiResponse<PaginationResponse<ComplainResponseDTO>> response =
                complainService.getComplains(null, 0, 10, "createdAt", "desc");

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(200, response.getStatusCode());

        PaginationResponse<ComplainResponseDTO> pagination = response.getData();
        assertNotNull(pagination);
        assertEquals(1, pagination.getContent().size());
        assertEquals(1, pagination.getTotalElements());
        assertEquals(1, pagination.getTotalPages());
        assertEquals(0, pagination.getPageNumber());
        assertEquals("Pothole on 5th Ave", pagination.getContent().get(0).getMessage());
        assertEquals("John Doe", pagination.getContent().get(0).getCitizenName());
    }

    @Test
    void testGetComplains_ForCitizen_WithStatusFilter() {
        when(securityUtils.getCurrentUser()).thenReturn(sampleUser);
        when(securityUtils.getCurrentUserRoles()).thenReturn(Set.of("ROLE_CITIZEN"));

        Page<Complains> complainPage = new PageImpl<>(List.of(sampleComplain));
        when(complainRepo.findByProfileAndComplainStatus(eq(sampleProfile), eq(ComplainStatus.PENDING), any(Pageable.class)))
                .thenReturn(complainPage);

        CommonApiResponse<PaginationResponse<ComplainResponseDTO>> response =
                complainService.getComplains(ComplainStatus.PENDING, 0, 5, "createdAt", "asc");

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(1, response.getData().getTotalElements());
        verify(complainRepo).findByProfileAndComplainStatus(eq(sampleProfile), eq(ComplainStatus.PENDING), any(Pageable.class));
    }
}
