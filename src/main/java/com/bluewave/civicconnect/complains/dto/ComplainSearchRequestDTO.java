package com.bluewave.civicconnect.complains.dto;

import com.bluewave.civicconnect.complains.constatns.ComplainStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ComplainSearchRequestDTO {
    private String keyword;
    private ComplainStatus status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private int page = 0;
    private int size = 10;
}
