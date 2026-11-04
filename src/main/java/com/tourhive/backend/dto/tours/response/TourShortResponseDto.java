package com.tourhive.backend.dto.tours.response;

import com.tourhive.backend.entities.tours.TourStatus;
import com.tourhive.backend.entities.tours.TourType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public record TourShortResponseDto(
        UUID id,
        UUID organizerId,
        String organizerName,
        String organizerAvatarUrl,
        String title,
        String description,
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
        List<String> includes,
        List<TourRouteItemResponseDto> route,
        String coverImageUrl,
        Boolean isFavorite
) {
    public TourShortResponseDto(
            UUID id,
            UUID organizerId,
            String organizerName,
            String organizerAvatarUrl,
            String title,
            String description,
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
            List<String> includes,
            List<TourRouteItemResponseDto> route,
            String coverImageUrl
    ) {
        this(
                id,
                organizerId,
                organizerName,
                organizerAvatarUrl,
                title,
                description,
                tourType,
                status,
                startDate,
                endDate,
                capacity,
                availablePlaces,
                price,
                currency,
                countries,
                cities,
                includes,
                route,
                coverImageUrl,
                false
        );
    }

    public TourShortResponseDto(
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
        this(
                id,
                organizerId,
                organizerName,
                organizerAvatarUrl,
                title,
                null,
                tourType,
                status,
                startDate,
                endDate,
                capacity,
                availablePlaces,
                price,
                currency,
                countries,
                cities,
                Collections.emptyList(),
                Collections.emptyList(),
                coverImageUrl,
                false
        );
    }
}
