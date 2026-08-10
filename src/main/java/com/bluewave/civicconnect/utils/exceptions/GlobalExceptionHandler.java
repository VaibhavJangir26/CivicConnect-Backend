package com.bluewave.civicconnect.utils.exceptions;

import com.bluewave.civicconnect.utils.common.CommonErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonErrorResponse<Map<String,String>>> handleValidationException(MethodArgumentNotValidException ex){
        Map<String,String> e=new HashMap<>();
        for(FieldError error:ex.getBindingResult().getFieldErrors()){
            e.put(error.getField(),error.getDefaultMessage());
        }
        CommonErrorResponse<Map<String, String>> response = CommonErrorResponse.<Map<String, String>>builder()
                .message("Validation failed for request parameters")
                .success(false)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .data(e)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<CommonErrorResponse<Void>> handleResourceConflict(ResourceConflictException ex){
        CommonErrorResponse<Void> res=CommonErrorResponse.<Void>builder()
                .message(ex.getMessage())
                .success(false)
                .statusCode(HttpStatus.CONFLICT.value())
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(res);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<CommonErrorResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex){
        CommonErrorResponse<Void> res=CommonErrorResponse.<Void>builder()
                .message(ex.getMessage())
                .success(false)
                .statusCode(HttpStatus.NOT_FOUND.value())
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(res);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<CommonErrorResponse<Void>> handleBadRequest(BadRequestException ex){
        CommonErrorResponse<Void> res=CommonErrorResponse.<Void>builder()
                .message(ex.getMessage())
                .success(false)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<CommonErrorResponse<Void>> handleUnauthorised(UnauthorizedException ex){
        CommonErrorResponse<Void> res=CommonErrorResponse.<Void>builder()
                .message(ex.getMessage())
                .success(false)
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(res);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<CommonErrorResponse<Void>> handleForbidden(ForbiddenException ex){
        CommonErrorResponse<Void> res=CommonErrorResponse.<Void>builder()
                .message(ex.getMessage())
                .success(false)
                .statusCode(HttpStatus.FORBIDDEN.value())
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(res);
    }

    @ExceptionHandler(GeneralException.class)
    public ResponseEntity<CommonErrorResponse<Void>> handleGeneral(GeneralException ex){
        CommonErrorResponse<Void> res=CommonErrorResponse.<Void>builder()
                .message(ex.getMessage())
                .success(false)
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(res);
    }

}
