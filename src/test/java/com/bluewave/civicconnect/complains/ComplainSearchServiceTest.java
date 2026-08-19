package com.bluewave.civicconnect.complains;

import com.bluewave.civicconnect.category.Categories;
import com.bluewave.civicconnect.category.CategoryTypes;
import com.bluewave.civicconnect.complains.constatns.ComplainPriority;
import com.bluewave.civicconnect.complains.constatns.ComplainStatus;
import com.bluewave.civicconnect.profile.Address;
import com.bluewave.civicconnect.profile.Profile;
import com.bluewave.civicconnect.users.Users;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ComplainSearchServiceTest {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @Mock
    private ComplainSearchRepository complainSearchRepository;

    @InjectMocks
    private ComplainSearchService complainSearchService;

    private Complains sampleComplain;

    @BeforeEach
    void setUp() {
        Categories sampleCategory = new Categories();
        sampleCategory.setId("cat-1");
        sampleCategory.setCategoryName("Sanitation");
        sampleCategory.setCategoryTypes(CategoryTypes.WATER_SANITATION);
        sampleCategory.setComplains(new ArrayList<>());

        Profile sampleProfile = new Profile();
        sampleProfile.setId("profile-1");
        sampleProfile.setFullName("Jane Citizen");

        Users sampleUser = new Users();
        sampleUser.setId("user-1");
        sampleUser.setUsername("janecitizen");
        sampleProfile.setUsers(sampleUser);

        Profile officerProfile = new Profile();
        officerProfile.setId("officer-1");
        officerProfile.setFullName("Officer Smith");

        Address address = new Address("Springfield", "USA", "IL", "62701", "456 Elm St");

        sampleComplain = new Complains();
        sampleComplain.setId("comp-99");
        sampleComplain.setMessage("Garbage overflow near park");
        sampleComplain.setAddress(address);
        sampleComplain.setCategory(sampleCategory);
        sampleComplain.setProfile(sampleProfile);
        sampleComplain.setAssignedOfficer(officerProfile);
        sampleComplain.setComplainPriority(ComplainPriority.MEDIUM);
        sampleComplain.setComplainStatus(ComplainStatus.IN_PROGRESS);
        sampleComplain.setCreatedAt(LocalDateTime.now());
        sampleComplain.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void testMapToSearchDocument() {
        ComplainSearchDocument doc = complainSearchService.mapToSearchDocument(sampleComplain);

        assertNotNull(doc);
        assertEquals("comp-99", doc.getId());
        assertEquals("Garbage overflow near park", doc.getMessage());
        assertEquals("Sanitation", doc.getCategoryName());
        assertEquals("IN_PROGRESS", doc.getComplainStatus());
        assertEquals("MEDIUM", doc.getComplainPriority());
        assertEquals("Jane Citizen", doc.getCitizenName());
        assertEquals("janecitizen", doc.getCitizenUsername());
        assertEquals("Officer Smith", doc.getAssignedOfficerName());
        assertTrue(doc.getAddress().contains("456 Elm St"));
        assertTrue(doc.getAddress().contains("Springfield"));
    }

    @Test
    void testIndexComplain() {
        complainSearchService.indexComplain(sampleComplain);
        verify(complainSearchRepository).save(any(ComplainSearchDocument.class));
    }

    @Test
    void testDeleteComplainFromIndex() {
        complainSearchService.deleteComplainFromIndex("comp-99");
        verify(complainSearchRepository).deleteById("comp-99");
    }
}
