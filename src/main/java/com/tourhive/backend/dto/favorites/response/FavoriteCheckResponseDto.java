package com.tourhive.backend.dto.favorites.response;

import java.util.UUID;

public record FavoriteCheckResponseDto(
        UUID tourId,
        boolean isFavorite
) {
}
