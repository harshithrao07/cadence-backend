package com.project.musicplayer.utility;

import com.project.musicplayer.dto.ApiResponseDTO;
import org.apache.catalina.connector.ClientAbortException;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {
    private final ErrorMapper errorMapper;

    public GlobalExceptionHandler(
            ErrorMapper errorMapper
    ) {
        this.errorMapper = errorMapper;
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ApiResponseDTO<Object>> handleThrowable(Throwable e) {
        // Handle socket closure scenario
        if (e instanceof ClientAbortException) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseDTO<>(false, "Client connection was aborted.", null));
        }

        // Handle general Throwable
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO<>(false, "An unexpected error occurred.", this.errorMapper.createErrorMap(e)));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponseDTO<Object>> handleRuntimeException(RuntimeException e) {
        // Specifically handle runtime exceptions
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO<>(false, "An unexpected error occurred.", this.errorMapper.createErrorMap(e)));
    }



    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseDTO<Object>> handleMethodArgumentNotValidException(@NotNull MethodArgumentNotValidException e) {
        Map<String, String> errorMap = new HashMap<>();

        e.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName;
            try {
                fieldName = ((FieldError) error).getField();
            } catch (ClassCastException ex) {
                fieldName = error.getObjectName();
            }
            String message = error.getDefaultMessage();
            errorMap.put(fieldName, message);
        });

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponseDTO<>(false, "Validation failed", errorMap));
    }

}