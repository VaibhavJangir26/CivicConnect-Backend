package com.bluewave.civicconnect.utils.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CommonErrorResponse<T>{

    private String message;
    @Builder.Default
    private boolean success=false;
    private int statusCode;
    private T data;
    private LocalDateTime timestamp;

}
