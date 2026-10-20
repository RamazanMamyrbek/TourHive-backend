package com.tourhive.backend.controllers.favorites;

import com.tourhive.backend.configurations.security.UserPrincipal;
import com.tourhive.backend.dto.common.ApiResponseDto;
import com.tourhive.backend.dto.favorites.response.FavoriteCheckResponseDto;
import com.tourhive.backend.dto.tours.response.TourShortResponseDto;
import com.tourhive.backend.services.favorites.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Favorites", description = "Endpoints for managing user favorite tours and wishlist")
@RestController
@RequestMapping("/api/v1/favorites/tours")
@RequiredArgsConstructor
public class FavoriteTourController {

    private final FavoriteService favoriteService;

    @Operation(summary = "Add tour to favorites", description = "Adds a published tour to user favorites", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{tourId}")
    public ResponseEntity<ApiResponseDto<Void>> addFavorite(
            @PathVariable UUID tourId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        favoriteService.addFavoriteTour(principal.getId(), tourId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("Tour added to favorites successfully", null));
    }

    @Operation(summary = "Remove tour from favorites", description = "Removes a tour from user favorites", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{tourId}")
    public ResponseEntity<Void> removeFavorite(
            @PathVariable UUID tourId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        favoriteService.removeFavoriteTour(principal.getId(), tourId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get favorite tours", description = "Returns paginated list of user favorite tours", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    public ResponseEntity<ApiResponseDto<Page<TourShortResponseDto>>> getFavorites(
            @AuthenticationPrincipal UserPrincipal principal,
            @ParameterObject Pageable pageable
    ) {
        Page<TourShortResponseDto> response = favoriteService.getFavoriteTours(principal.getId(), pageable);
        return ResponseEntity.ok(ApiResponseDto.success(response));
    }

    @Operation(summary = "Check favorite status", description = "Checks whether a tour is in user favorites", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{tourId}/check")
    public ResponseEntity<ApiResponseDto<FavoriteCheckResponseDto>> checkFavorite(
            @PathVariable UUID tourId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        FavoriteCheckResponseDto response = favoriteService.checkFavoriteTour(principal.getId(), tourId);
        return ResponseEntity.ok(ApiResponseDto.success(response));
    }
}
