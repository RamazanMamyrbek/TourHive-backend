package com.tourhive.backend.dto.tours.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TourRouteItemRequestDto(
        @NotBlank(message = "Route stop city is required")
        @Size(max = 100, message = "City name cannot exceed 100 characters")
        String city,

        LocalDate routeDate,

        String description,

        Integer orderIndex
) {
}
