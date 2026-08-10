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
public class CommonApiResponse<T> {

    private String message;
    private boolean success;
    private int statusCode;
    private T data;
    @Builder.Default
    private LocalDateTime timestamp=LocalDateTime.now();

}
