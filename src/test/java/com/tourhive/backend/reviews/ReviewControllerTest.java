package com.tourhive.backend.reviews;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourhive.backend.configurations.security.JwtTokenProvider;
import com.tourhive.backend.dto.reviews.request.CreateReviewRequestDto;
import com.tourhive.backend.dto.reviews.request.UpdateReviewRequestDto;
import com.tourhive.backend.entities.organizers.OrganizerProfile;
import com.tourhive.backend.entities.reviews.Review;
import com.tourhive.backend.entities.tours.Tour;
import com.tourhive.backend.entities.tours.TourApplication;
import com.tourhive.backend.entities.tours.TourApplicationStatus;
import com.tourhive.backend.entities.tours.TourStatus;
import com.tourhive.backend.entities.tours.TourType;
import com.tourhive.backend.entities.users.Role;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserProfile;
import com.tourhive.backend.entities.users.UserStatus;
import com.tourhive.backend.repositories.organizers.OrganizerProfileRepository;
import com.tourhive.backend.repositories.reviews.ReviewRepository;
import com.tourhive.backend.repositories.tours.TourApplicationRepository;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DatabaseCleanup databaseCleanup;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private OrganizerProfileRepository organizerProfileRepository;

    @Autowired
    private TourRepository tourRepository;

    @Autowired
    private TourApplicationRepository tourApplicationRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User organizer;
    private User traveler;
    private User stranger;
    private String travelerToken;
    private String strangerToken;
    private Tour completedTour;
    private Tour upcomingTour;

    @BeforeEach
    void setUp() {
        databaseCleanup.clean();

        organizer = createUser("org@test.com", Role.ORGANIZER);
        organizerProfileRepository.save(OrganizerProfile.builder().user(organizer).rating(BigDecimal.ZERO).reviewsCount(0).build());

        traveler = createUser("traveler@test.com", Role.USER);
        stranger = createUser("stranger@test.com", Role.USER);

        travelerToken = jwtTokenProvider.generateAccessToken(traveler);
        strangerToken = jwtTokenProvider.generateAccessToken(stranger);

        completedTour = Tour.builder()
                .organizer(organizer)
                .title("Past Tian Shan Trek")
                .description("Scenic mountain tour")
                .tourType(TourType.COMMUNITY)
                .status(TourStatus.PUBLISHED)
                .startDate(LocalDate.now().minusDays(10))
                .endDate(LocalDate.now().minusDays(2))
                .capacity(10)
                .availablePlaces(9)
                .price(BigDecimal.valueOf(15000))
                .currency("KZT")
                .build();
        completedTour = tourRepository.save(completedTour);

        upcomingTour = Tour.builder()
                .organizer(organizer)
                .title("Upcoming Tian Shan Trek")
                .description("Future tour")
                .tourType(TourType.COMMUNITY)
                .status(TourStatus.PUBLISHED)
                .startDate(LocalDate.now().plusDays(2))
                .endDate(LocalDate.now().plusDays(10))
                .capacity(10)
                .availablePlaces(9)
                .price(BigDecimal.valueOf(15000))
                .currency("KZT")
                .build();
        upcomingTour = tourRepository.save(upcomingTour);

        tourApplicationRepository.save(TourApplication.builder()
                .tour(completedTour)
                .applicant(traveler)
                .status(TourApplicationStatus.ACCEPTED)
                .build());

        tourApplicationRepository.save(TourApplication.builder()
                .tour(upcomingTour)
                .applicant(traveler)
                .status(TourApplicationStatus.ACCEPTED)
                .build());
    }

    private User createUser(String email, Role role) {
        User user = User.builder()
                .email(email)
                .passwordHash("hashed")
                .role(role)
                .status(UserStatus.ACTIVE)
                .build();
        user = userRepository.save(user);

        UserProfile profile = UserProfile.builder()
                .user(user)
                .firstName(email.split("@")[0])
                .nickname(email.split("@")[0] + "_nick")
                .build();
        userProfileRepository.save(profile);
        return user;
    }

    @Test
    @DisplayName("Create review requires authentication")
    void createReviewRequiresAuth() throws Exception {
        CreateReviewRequestDto dto = new CreateReviewRequestDto(5, "Great experience!");

        mockMvc.perform(post("/api/v1/tours/" + completedTour.getId() + "/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Accepted traveler can review completed tour")
    void acceptedTravelerCanReviewCompletedTour() throws Exception {
        CreateReviewRequestDto dto = new CreateReviewRequestDto(5, "Spectacular tour and guide!");

        mockMvc.perform(post("/api/v1/tours/" + completedTour.getId() + "/reviews")
                        .header("Authorization", "Bearer " + travelerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.rating", is(5)))
                .andExpect(jsonPath("$.data.comment", is("Spectacular tour and guide!")));
    }

    @Test
    @DisplayName("Cannot review upcoming/unfinished tour")
    void cannotReviewUnfinishedTour() throws Exception {
        CreateReviewRequestDto dto = new CreateReviewRequestDto(5, "Too early");

        mockMvc.perform(post("/api/v1/tours/" + upcomingTour.getId() + "/reviews")
                        .header("Authorization", "Bearer " + travelerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("TOUR_NOT_COMPLETED")));
    }

    @Test
    @DisplayName("Stranger without accepted application cannot review tour")
    void strangerCannotReviewTour() throws Exception {
        CreateReviewRequestDto dto = new CreateReviewRequestDto(5, "I was not there");

        mockMvc.perform(post("/api/v1/tours/" + completedTour.getId() + "/reviews")
                        .header("Authorization", "Bearer " + strangerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("REVIEW_ACCESS_DENIED")));
    }

    @Test
    @DisplayName("Duplicate review on same tour returns Conflict")
    void duplicateReviewReturnsConflict() throws Exception {
        CreateReviewRequestDto dto = new CreateReviewRequestDto(5, "First review");

        mockMvc.perform(post("/api/v1/tours/" + completedTour.getId() + "/reviews")
                        .header("Authorization", "Bearer " + travelerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/tours/" + completedTour.getId() + "/reviews")
                        .header("Authorization", "Bearer " + travelerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("DUPLICATE_REVIEW")));
    }

    @Test
    @DisplayName("Public can view tour reviews")
    void publicCanViewTourReviews() throws Exception {
        reviewRepository.save(Review.builder()
                .tour(completedTour)
                .organizer(organizer)
                .author(traveler)
                .rating(5)
                .comment("Excellent")
                .build());

        mockMvc.perform(get("/api/v1/tours/" + completedTour.getId() + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].rating", is(5)));
    }

    @Test
    @DisplayName("Author can update review")
    void authorCanUpdateReview() throws Exception {
        Review review = reviewRepository.save(Review.builder()
                .tour(completedTour)
                .organizer(organizer)
                .author(traveler)
                .rating(4)
                .comment("Initial comment")
                .build());

        UpdateReviewRequestDto dto = new UpdateReviewRequestDto(5, "Updated to 5 stars");

        mockMvc.perform(put("/api/v1/reviews/" + review.getId())
                        .header("Authorization", "Bearer " + travelerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rating", is(5)))
                .andExpect(jsonPath("$.data.comment", is("Updated to 5 stars")));
    }

    @Test
    @DisplayName("Author can delete review")
    void authorCanDeleteReview() throws Exception {
        Review review = reviewRepository.save(Review.builder()
                .tour(completedTour)
                .organizer(organizer)
                .author(traveler)
                .rating(4)
                .comment("To be deleted")
                .build());

        mockMvc.perform(delete("/api/v1/reviews/" + review.getId())
                        .header("Authorization", "Bearer " + travelerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/tours/" + completedTour.getId() + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));
    }
}
