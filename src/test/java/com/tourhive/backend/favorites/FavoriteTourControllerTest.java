package com.tourhive.backend.favorites;

import com.tourhive.backend.configurations.security.JwtTokenProvider;
import com.tourhive.backend.entities.favorites.Favorite;
import com.tourhive.backend.entities.tours.Tour;
import com.tourhive.backend.entities.tours.TourStatus;
import com.tourhive.backend.entities.tours.TourType;
import com.tourhive.backend.entities.users.Role;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserProfile;
import com.tourhive.backend.entities.users.UserStatus;
import com.tourhive.backend.repositories.favorites.FavoriteRepository;
import com.tourhive.backend.repositories.tours.TourRepository;
import com.tourhive.backend.repositories.users.UserProfileRepository;
import com.tourhive.backend.repositories.users.UserRepository;
import com.tourhive.backend.support.DatabaseCleanup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FavoriteTourControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private TourRepository tourRepository;

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private DatabaseCleanup databaseCleanup;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User traveler;
    private User organizer;
    private Tour publishedTour;
    private Tour draftTour;
    private String tokenTraveler;

    @BeforeEach
    void setUp() {
        databaseCleanup.clean();

        traveler = userRepository.save(User.builder()
                .email("traveler@tourhive.com")
                .passwordHash("hash")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build());

        organizer = userRepository.save(User.builder()
                .email("organizer@tourhive.com")
                .passwordHash("hash")
                .role(Role.ORGANIZER)
                .status(UserStatus.ACTIVE)
                .isOrganizer(true)
                .build());

        userProfileRepository.save(UserProfile.builder().user(traveler).nickname("traveler").build());
        userProfileRepository.save(UserProfile.builder().user(organizer).nickname("organizer").build());

        tokenTraveler = "Bearer " + jwtTokenProvider.generateAccessToken(traveler);

        publishedTour = tourRepository.save(Tour.builder()
                .organizer(organizer)
                .title("Kaindy Lake Adventure")
                .description("Scenic tour to sunken forest")
                .tourType(TourType.COMMUNITY)
                .status(TourStatus.PUBLISHED)
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(12))
                .capacity(15)
                .availablePlaces(15)
                .price(BigDecimal.valueOf(35000))
                .currency("KZT")
                .countries(List.of("Kazakhstan"))
                .cities(List.of("Almaty"))
                .build());

        draftTour = tourRepository.save(Tour.builder()
                .organizer(organizer)
                .title("Draft Mountain Trek")
                .description("Unpublished draft")
                .tourType(TourType.COMMUNITY)
                .status(TourStatus.DRAFT)
                .startDate(LocalDate.now().plusDays(20))
                .endDate(LocalDate.now().plusDays(22))
                .capacity(10)
                .availablePlaces(10)
                .price(BigDecimal.valueOf(25000))
                .currency("KZT")
                .build());
    }

    @Test
    @DisplayName("Should add tour to favorites successfully")
    void shouldAddTourToFavoritesSuccessfully() throws Exception {
        mockMvc.perform(post("/api/v1/favorites/tours/" + publishedTour.getId())
                        .header("Authorization", tokenTraveler))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message", is("Tour added to favorites successfully")));

        assertThat(favoriteRepository.existsByUserIdAndTourId(traveler.getId(), publishedTour.getId())).isTrue();
    }

    @Test
    @DisplayName("Should reject favorite when tour is already in favorites")
    void shouldRejectFavoriteWhenAlreadyFavorited() throws Exception {
        favoriteRepository.save(Favorite.builder()
                .user(traveler)
                .tour(publishedTour)
                .build());

        mockMvc.perform(post("/api/v1/favorites/tours/" + publishedTour.getId())
                        .header("Authorization", tokenTraveler))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("DUPLICATE_FAVORITE")));
    }

    @Test
    @DisplayName("Should reject favorite when tour is not published")
    void shouldRejectFavoriteWhenTourNotPublished() throws Exception {
        mockMvc.perform(post("/api/v1/favorites/tours/" + draftTour.getId())
                        .header("Authorization", tokenTraveler))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_TOUR_DATA")));
    }

    @Test
    @DisplayName("Should check favorite status")
    void shouldCheckFavoriteStatus() throws Exception {
        mockMvc.perform(get("/api/v1/favorites/tours/" + publishedTour.getId() + "/check")
                        .header("Authorization", tokenTraveler))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isFavorite", is(false)));

        favoriteRepository.save(Favorite.builder()
                .user(traveler)
                .tour(publishedTour)
                .build());

        mockMvc.perform(get("/api/v1/favorites/tours/" + publishedTour.getId() + "/check")
                        .header("Authorization", tokenTraveler))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isFavorite", is(true)));
    }

    @Test
    @DisplayName("Should list favorite tours")
    void shouldListFavoriteTours() throws Exception {
        favoriteRepository.save(Favorite.builder()
                .user(traveler)
                .tour(publishedTour)
                .build());

        mockMvc.perform(get("/api/v1/favorites/tours")
                        .header("Authorization", tokenTraveler))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].id", is(publishedTour.getId().toString())))
                .andExpect(jsonPath("$.data.content[0].title", is("Kaindy Lake Adventure")));
    }

    @Test
    @DisplayName("Should remove tour from favorites")
    void shouldRemoveTourFromFavorites() throws Exception {
        favoriteRepository.save(Favorite.builder()
                .user(traveler)
                .tour(publishedTour)
                .build());

        mockMvc.perform(delete("/api/v1/favorites/tours/" + publishedTour.getId())
                        .header("Authorization", tokenTraveler))
                .andExpect(status().isNoContent());

        assertThat(favoriteRepository.existsByUserIdAndTourId(traveler.getId(), publishedTour.getId())).isFalse();
    }
}
