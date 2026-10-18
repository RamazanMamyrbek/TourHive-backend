package com.tourhive.backend.dto.tours.response;

import java.util.UUID;

public record TourImageResponseDto(
        UUID id,
        UUID resourceId,
        String url,
        Boolean isCover,
        Integer orderIndex
) {
}
