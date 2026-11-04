package com.tourhive.backend.dto.tours.response;

import com.tourhive.backend.entities.tours.TourStatus;
import com.tourhive.backend.entities.tours.TourType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TourResponseDto(
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
        List<TourImageResponseDto> images,
        String coverImageUrl,
        String rejectionReason,
        String cancellationReason,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt,
        Boolean isFavorite
) {
    public TourResponseDto(
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
            List<TourImageResponseDto> images,
            String coverImageUrl,
            String rejectionReason,
            String cancellationReason,
            Instant publishedAt,
            Instant createdAt,
            Instant updatedAt
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
                images,
                coverImageUrl,
                rejectionReason,
                cancellationReason,
                publishedAt,
                createdAt,
                updatedAt,
                false
        );
    }
}
