package com.tourhive.backend.dto.tours.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddTourImageRequestDto(
        @NotNull(message = "Resource ID is required")
        UUID resourceId,

        Boolean isCover,

        Integer orderIndex
) {
}
