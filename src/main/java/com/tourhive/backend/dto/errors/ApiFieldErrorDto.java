package com.tourhive.backend.dto.errors;

public record ApiFieldErrorDto(
        String field,
        String message,
        Object rejectedValue
) {
}
