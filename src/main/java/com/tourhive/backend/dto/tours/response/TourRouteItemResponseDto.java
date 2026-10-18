package com.tourhive.backend.dto.tours.response;

import java.time.LocalDate;
import java.util.UUID;

public record TourRouteItemResponseDto(
        UUID id,
        String city,
        LocalDate routeDate,
        String description,
        Integer orderIndex
) {
}
