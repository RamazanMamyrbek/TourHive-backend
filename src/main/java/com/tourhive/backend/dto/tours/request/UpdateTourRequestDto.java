package com.tourhive.backend.dto.tours.request;

import com.tourhive.backend.entities.tours.TourType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record UpdateTourRequestDto(
        @Size(max = 255, message = "Tour title cannot exceed 255 characters")
        String title,

        String description,

        TourType tourType,

        @FutureOrPresent(message = "Start date must not be in the past")
        LocalDate startDate,

        LocalDate endDate,

        @Min(value = 1, message = "Capacity must be at least 1")
        Integer capacity,

        @Min(value = 0, message = "Price cannot be negative")
        BigDecimal price,

        @Size(max = 10, message = "Currency code must not exceed 10 characters")
        String currency,

        List<String> countries,

        List<String> cities,

        List<String> includes,

        List<@Valid TourRouteItemRequestDto> route
) {
}
