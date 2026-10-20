package com.tourhive.backend.services.favorites.impl;

import com.tourhive.backend.dto.favorites.response.FavoriteCheckResponseDto;
import com.tourhive.backend.dto.tours.response.TourShortResponseDto;
import com.tourhive.backend.entities.favorites.Favorite;
import com.tourhive.backend.entities.tours.Tour;
import com.tourhive.backend.entities.tours.TourStatus;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserProfile;
import com.tourhive.backend.exceptions.DuplicateFavoriteException;
import com.tourhive.backend.exceptions.FavoriteNotFoundException;
import com.tourhive.backend.exceptions.InvalidTourDataException;
import com.tourhive.backend.exceptions.TourNotFoundException;
import com.tourhive.backend.mappers.tours.TourMapper;
import com.tourhive.backend.repositories.favorites.FavoriteRepository;
import com.tourhive.backend.repositories.tours.TourRepository;
import com.tourhive.backend.repositories.users.UserProfileRepository;
import com.tourhive.backend.services.favorites.FavoriteService;
import com.tourhive.backend.services.users.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final TourRepository tourRepository;
    private final UserService userService;
    private final UserProfileRepository userProfileRepository;
    private final TourMapper tourMapper;

    @Override
    @Transactional
    public void addFavoriteTour(UUID userId, UUID tourId) {
        User user = userService.getById(userId);
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new TourNotFoundException("Tour not found: " + tourId));

        if (tour.getStatus() != TourStatus.PUBLISHED) {
            throw new InvalidTourDataException("Only published tours can be added to favorites");
        }

        if (favoriteRepository.existsByUserIdAndTourId(userId, tourId)) {
            throw new DuplicateFavoriteException("Tour is already in your favorites");
        }

        Favorite favorite = Favorite.builder()
                .user(user)
                .tour(tour)
                .build();

        favoriteRepository.save(favorite);
        log.info("User {} added tour {} to favorites", userId, tourId);
    }

    @Override
    @Transactional
    public void removeFavoriteTour(UUID userId, UUID tourId) {
        Favorite favorite = favoriteRepository.findByUserIdAndTourId(userId, tourId)
                .orElseThrow(() -> new FavoriteNotFoundException("Tour is not in your favorites"));

        favoriteRepository.delete(favorite);
        log.info("User {} removed tour {} from favorites", userId, tourId);
    }

    @Override
    public Page<TourShortResponseDto> getFavoriteTours(UUID userId, Pageable pageable) {
        Page<Favorite> favorites = favoriteRepository.findAllByUserIdOrderByCreatedAtDesc(userId, pageable);
        return favorites.map(fav -> {
            Tour tour = fav.getTour();
            UserProfile organizerProfile = userProfileRepository.findByUserId(tour.getOrganizer().getId()).orElse(null);
            return tourMapper.toShortResponseDto(tour, organizerProfile);
        });
    }

    @Override
    public FavoriteCheckResponseDto checkFavoriteTour(UUID userId, UUID tourId) {
        boolean isFav = favoriteRepository.existsByUserIdAndTourId(userId, tourId);
        return new FavoriteCheckResponseDto(tourId, isFav);
    }
}
