package com.tourhive.backend.dto.errors;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponseDto(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        List<ApiFieldErrorDto> fieldErrors
) {
    public static ApiErrorResponseDto of(int status, String code, String message, String path) {
        return new ApiErrorResponseDto(Instant.now(), status, code, message, path, null);
    }

    public static ApiErrorResponseDto of(int status, String code, String message, String path, List<ApiFieldErrorDto> fieldErrors) {
        return new ApiErrorResponseDto(Instant.now(), status, code, message, path, fieldErrors);
    }
}
