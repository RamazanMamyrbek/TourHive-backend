package com.tourhive.backend.dto.common;

import java.time.Instant;

public record ApiResponseDto<T>(
        Instant timestamp,
        boolean success,
        String message,
        T data
) {
    public static <T> ApiResponseDto<T> success(T data) {
        return new ApiResponseDto<>(Instant.now(), true, "Operation completed successfully", data);
    }

    public static <T> ApiResponseDto<T> success(String message, T data) {
        return new ApiResponseDto<>(Instant.now(), true, message, data);
    }
}
