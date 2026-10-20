package com.tourhive.backend.services.favorites;

import com.tourhive.backend.dto.favorites.response.FavoriteCheckResponseDto;
import com.tourhive.backend.dto.tours.response.TourShortResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface FavoriteService {

    void addFavoriteTour(UUID userId, UUID tourId);

    void removeFavoriteTour(UUID userId, UUID tourId);

    Page<TourShortResponseDto> getFavoriteTours(UUID userId, Pageable pageable);

    FavoriteCheckResponseDto checkFavoriteTour(UUID userId, UUID tourId);
}
