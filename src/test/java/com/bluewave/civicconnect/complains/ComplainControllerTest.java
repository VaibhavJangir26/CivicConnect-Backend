package com.bluewave.civicconnect.complains;

import com.bluewave.civicconnect.complains.constatns.ComplainPriority;
import com.bluewave.civicconnect.complains.constatns.ComplainStatus;
import com.bluewave.civicconnect.complains.dto.ComplainResponseDTO;
import com.bluewave.civicconnect.utils.common.CommonApiResponse;
import com.bluewave.civicconnect.utils.common.PaginationResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComplainControllerTest {

    @Mock
    private ComplainService complainService;

    @Mock
    private ComplainSearchService complainSearchService;

    @InjectMocks
    private ComplainController complainController;

    @Test
    void testGetComplains_ReturnsPaginatedResponse() {
        ComplainResponseDTO dto = ComplainResponseDTO.builder()
                .id("c-1")
                .message("Test message")
                .complainStatus(ComplainStatus.PENDING)
                .complainPriority(ComplainPriority.LOW)
                .createdAt(LocalDateTime.now())
                .build();

        PaginationResponse<ComplainResponseDTO> paginationResponse = PaginationResponse.<ComplainResponseDTO>builder()
                .content(List.of(dto))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .hasNext(false)
                .hasPrevious(false)
                .build();

        CommonApiResponse<PaginationResponse<ComplainResponseDTO>> apiResponse =
                CommonApiResponse.<PaginationResponse<ComplainResponseDTO>>builder()
                        .message("Complaints fetched successfully")
                        .data(paginationResponse)
                        .statusCode(HttpStatus.OK.value())
                        .success(true)
                        .timestamp(LocalDateTime.now())
                        .build();

        when(complainService.getComplains(null, 0, 10, "createdAt", "desc")).thenReturn(apiResponse);

        ResponseEntity<CommonApiResponse<PaginationResponse<ComplainResponseDTO>>> response =
                complainController.getComplains(null, 0, 10, "createdAt", "desc");

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals(1, response.getBody().getData().getTotalElements());
        assertEquals("c-1", response.getBody().getData().getContent().get(0).getId());
    }
}
