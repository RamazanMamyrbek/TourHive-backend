package com.tourhive.backend.dto.organizers.response;

import java.math.BigDecimal;
import java.util.UUID;

public record OrganizerProfileResponseDto(
        UUID id,
        UUID userId,
        String bio,
        int experienceYears,
        BigDecimal rating,
        int reviewsCount,
        boolean isOrganizer,
        boolean isBusinessVerified,
        BusinessProfileResponseDto businessProfile
) {
}
