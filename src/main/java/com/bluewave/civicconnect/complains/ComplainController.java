package com.bluewave.civicconnect.complains;

import com.bluewave.civicconnect.complains.constatns.ComplainStatus;
import com.bluewave.civicconnect.complains.dto.*;
import com.bluewave.civicconnect.utils.common.CommonApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/complains")
@RequiredArgsConstructor
public class ComplainController {

    private final ComplainService complainService;
    private final ComplainRepo.ComplainSearchService complainSearchService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<CommonApiResponse<ComplainResponseDTO>> createComplain(
            @Valid @RequestPart("complainData") ComplainRequestDTO dto,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        return ResponseEntity.status(HttpStatus.CREATED).body(complainService.createComplain(dto, images));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CITIZEN', 'OFFICER', 'MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<CommonApiResponse<List<ComplainResponseDTO>>> getComplains(
            @RequestParam(required = false) ComplainStatus status) {
        return ResponseEntity.ok(complainService.getComplains(status));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('CITIZEN', 'OFFICER', 'MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<CommonApiResponse<List<ComplainRepo.ComplainSearchDocument>>> searchComplains(
            @ModelAttribute ComplainSearchRequestDTO dto) {
        List<ComplainRepo.ComplainSearchDocument> results = complainSearchService.globalSearch(dto);
        return ResponseEntity.ok(CommonApiResponse.<List<ComplainRepo.ComplainSearchDocument>>builder()
                .message("Search results fetched successfully")
                .statusCode(HttpStatus.OK.value())
                .success(true)
                .data(results)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/suggest")
    @PreAuthorize("hasAnyRole('CITIZEN', 'OFFICER', 'MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<CommonApiResponse<List<String>>> autosuggestComplains(
            @RequestParam("query") String query) {
        List<String> suggestions = complainSearchService.autosuggest(query);
        return ResponseEntity.ok(CommonApiResponse.<List<String>>builder()
                .message("Autosuggestions fetched successfully")
                .statusCode(HttpStatus.OK.value())
                .success(true)
                .data(suggestions)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CITIZEN', 'OFFICER', 'MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<CommonApiResponse<ComplainResponseDTO>> getComplainDetails(@PathVariable String id) {
        return ResponseEntity.ok(complainService.getComplainDetails(id));
    }

    @PatchMapping(value = "/status", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('OFFICER', 'SUPER_ADMIN')")
    public ResponseEntity<CommonApiResponse<ComplainResponseDTO>> updateComplainStatus(
            @Valid @RequestPart("updateData") ComplainUpdateRequestLowLevelDTO dto,
            @RequestPart(value = "proofImages", required = false) List<MultipartFile> proofImages) {
        return ResponseEntity.ok(complainService.updateComplainStatus(dto, proofImages));
    }

    @PatchMapping("/manage")
    @PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<CommonApiResponse<ComplainResponseDTO>> manageComplain(
            @Valid @RequestBody ComplainUpdateRequestHighLevelDTO dto) {
        return ResponseEntity.ok(complainService.manageComplain(dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<CommonApiResponse<String>> deleteComplain(@PathVariable String id) {
        return ResponseEntity.ok(complainService.deleteComplain(id));
    }
}