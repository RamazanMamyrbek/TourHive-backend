package com.tourhive.backend.favorites;

import com.tourhive.backend.dto.favorites.response.FavoriteCheckResponseDto;
import com.tourhive.backend.dto.tours.response.TourShortResponseDto;
import com.tourhive.backend.entities.favorites.Favorite;
import com.tourhive.backend.entities.tours.Tour;
import com.tourhive.backend.entities.tours.TourStatus;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.exceptions.DuplicateFavoriteException;
import com.tourhive.backend.exceptions.FavoriteNotFoundException;
import com.tourhive.backend.exceptions.InvalidTourDataException;
import com.tourhive.backend.mappers.tours.TourMapper;
import com.tourhive.backend.repositories.favorites.FavoriteRepository;
import com.tourhive.backend.repositories.tours.TourRepository;
import com.tourhive.backend.repositories.users.UserProfileRepository;
import com.tourhive.backend.services.favorites.impl.FavoriteServiceImpl;
import com.tourhive.backend.services.users.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private TourRepository tourRepository;

    @Mock
    private UserService userService;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private TourMapper tourMapper;

    @InjectMocks
    private FavoriteServiceImpl favoriteService;

    private UUID userId;
    private UUID tourId;
    private User user;
    private Tour publishedTour;
    private Tour draftTour;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        tourId = UUID.randomUUID();

        user = User.builder().email("user@tourhive.com").build();
        user.setId(userId);

        User organizer = User.builder().email("organizer@tourhive.com").build();
        organizer.setId(UUID.randomUUID());

        publishedTour = Tour.builder()
                .organizer(organizer)
                .title("Almaty Mountains")
                .status(TourStatus.PUBLISHED)
                .build();
        publishedTour.setId(tourId);

        draftTour = Tour.builder()
                .organizer(organizer)
                .title("Draft Tour")
                .status(TourStatus.DRAFT)
                .build();
        draftTour.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Should throw InvalidTourDataException when tour is not published")
    void shouldThrowWhenTourNotPublished() {
        when(userService.getById(userId)).thenReturn(user);
        when(tourRepository.findById(draftTour.getId())).thenReturn(Optional.of(draftTour));

        assertThatThrownBy(() -> favoriteService.addFavoriteTour(userId, draftTour.getId()))
                .isInstanceOf(InvalidTourDataException.class)
                .hasMessageContaining("Only published tours can be added to favorites");

        verify(favoriteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw DuplicateFavoriteException when tour is already in favorites")
    void shouldThrowWhenDuplicateFavorite() {
        when(userService.getById(userId)).thenReturn(user);
        when(tourRepository.findById(tourId)).thenReturn(Optional.of(publishedTour));
        when(favoriteRepository.existsByUserIdAndTourId(userId, tourId)).thenReturn(true);

        assertThatThrownBy(() -> favoriteService.addFavoriteTour(userId, tourId))
                .isInstanceOf(DuplicateFavoriteException.class)
                .hasMessageContaining("already in your favorites");

        verify(favoriteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should add tour to favorites successfully")
    void shouldAddFavoriteSuccessfully() {
        when(userService.getById(userId)).thenReturn(user);
        when(tourRepository.findById(tourId)).thenReturn(Optional.of(publishedTour));
        when(favoriteRepository.existsByUserIdAndTourId(userId, tourId)).thenReturn(false);

        favoriteService.addFavoriteTour(userId, tourId);

        verify(favoriteRepository).save(any(Favorite.class));
    }

    @Test
    @DisplayName("Should throw FavoriteNotFoundException when removing non-existent favorite")
    void shouldThrowWhenRemovingNonExistentFavorite() {
        when(favoriteRepository.findByUserIdAndTourId(userId, tourId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> favoriteService.removeFavoriteTour(userId, tourId))
                .isInstanceOf(FavoriteNotFoundException.class)
                .hasMessageContaining("not in your favorites");

        verify(favoriteRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should remove tour from favorites successfully")
    void shouldRemoveFavoriteSuccessfully() {
        Favorite fav = Favorite.builder().user(user).tour(publishedTour).build();
        when(favoriteRepository.findByUserIdAndTourId(userId, tourId)).thenReturn(Optional.of(fav));

        favoriteService.removeFavoriteTour(userId, tourId);

        verify(favoriteRepository).delete(fav);
    }

    @Test
    @DisplayName("Should check favorite status correctly")
    void shouldCheckFavoriteStatusCorrectly() {
        when(favoriteRepository.existsByUserIdAndTourId(userId, tourId)).thenReturn(true);

        FavoriteCheckResponseDto check = favoriteService.checkFavoriteTour(userId, tourId);

        assertThat(check.tourId()).isEqualTo(tourId);
        assertThat(check.isFavorite()).isTrue();
    }
}
