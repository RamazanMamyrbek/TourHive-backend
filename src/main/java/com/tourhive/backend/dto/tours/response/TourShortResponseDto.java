package com.tourhive.backend.dto.tours.response;

import com.tourhive.backend.entities.tours.TourStatus;
import com.tourhive.backend.entities.tours.TourType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TourShortResponseDto(
        UUID id,
        UUID organizerId,
        String organizerName,
        String organizerAvatarUrl,
        String title,
        TourType tourType,
        TourStatus status,
        LocalDate startDate,
        LocalDate endDate,
        Integer capacity,
        Integer availablePlaces,
        BigDecimal price,
        String currency,
        List<String> countries,
        List<String> cities,
        String coverImageUrl
) {
}
