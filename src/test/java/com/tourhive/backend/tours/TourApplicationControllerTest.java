package com.tourhive.backend.tours;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourhive.backend.configurations.security.JwtTokenProvider;
import com.tourhive.backend.dto.tours.request.ApplyTourRequestDto;
import com.tourhive.backend.dto.tours.request.CancelApplicationRequestDto;
import com.tourhive.backend.dto.tours.request.ReviewApplicationRequestDto;
import com.tourhive.backend.entities.tours.Tour;
import com.tourhive.backend.entities.tours.TourApplication;
import com.tourhive.backend.entities.tours.TourApplicationStatus;
import com.tourhive.backend.entities.tours.TourStatus;
import com.tourhive.backend.entities.tours.TourType;
import com.tourhive.backend.entities.users.Role;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserStatus;
import com.tourhive.backend.repositories.tours.TourApplicationRepository;
import com.tourhive.backend.repositories.tours.TourRepository;
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
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TourApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TourRepository tourRepository;

    @Autowired
    private TourApplicationRepository tourApplicationRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private DatabaseCleanup databaseCleanup;

    private User organizer;
    private User traveler;
    private User stranger;
    private String organizerToken;
    private String travelerToken;
    private String strangerToken;
    private Tour tour;

    @BeforeEach
    void setUp() {
        databaseCleanup.clean();

        organizer = User.builder()
                .email("org_app_test@example.com")
                .passwordHash("hash")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .isOrganizer(true)
                .build();
        organizer = userRepository.save(organizer);
        organizerToken = jwtTokenProvider.generateAccessToken(organizer);

        traveler = User.builder()
                .email("trav_app_test@example.com")
                .passwordHash("hash")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .isOrganizer(false)
                .build();
        traveler = userRepository.save(traveler);
        travelerToken = jwtTokenProvider.generateAccessToken(traveler);

        stranger = User.builder()
                .email("stranger_app_test@example.com")
                .passwordHash("hash")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .isOrganizer(false)
                .build();
        stranger = userRepository.save(stranger);
        strangerToken = jwtTokenProvider.generateAccessToken(stranger);

        tour = Tour.builder()
                .organizer(organizer)
                .title("Tian Shan Mountain Trek")
                .description("Hike across Tien Shan peaks")
                .tourType(TourType.COMMUNITY)
                .status(TourStatus.PUBLISHED)
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(15))
                .capacity(10)
                .availablePlaces(10)
                .price(BigDecimal.valueOf(25000))
                .currency("KZT")
                .countries(new ArrayList<>(List.of("Kazakhstan")))
                .cities(new ArrayList<>(List.of("Almaty")))
                .build();
        tour = tourRepository.save(tour);
    }

    @Test
    @DisplayName("POST /api/v1/tours/{tourId}/applications - traveler submits application successfully")
    void shouldSubmitApplicationSuccessfully() throws Exception {
        ApplyTourRequestDto request = new ApplyTourRequestDto("I want to participate", "+77011112233");

        mockMvc.perform(post("/api/v1/tours/{tourId}/applications", tour.getId())
                        .header("Authorization", "Bearer " + travelerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.message").value("I want to participate"))
                .andExpect(jsonPath("$.data.contactInfo").value("+77011112233"));
    }

    @Test
    @DisplayName("GET /api/v1/tours/{tourId}/applications - organizer views tour applications")
    void shouldGetApplicationsForOrganizer() throws Exception {
        TourApplication app = TourApplication.builder()
                .tour(tour)
                .applicant(traveler)
                .status(TourApplicationStatus.PENDING)
                .message("Hello")
                .build();
        tourApplicationRepository.save(app);

        mockMvc.perform(get("/api/v1/tours/{tourId}/applications", tour.getId())
                        .header("Authorization", "Bearer " + organizerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("GET /api/v1/tours/{tourId}/applications - stranger denied access")
    void shouldDenyStrangerAccessToTourApplications() throws Exception {
        mockMvc.perform(get("/api/v1/tours/{tourId}/applications", tour.getId())
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_APPLICATION_ACCESS"));
    }

    @Test
    @DisplayName("POST /api/v1/tours/{tourId}/applications/{id}/review - organizer accepts application")
    void shouldAcceptApplicationByOrganizer() throws Exception {
        TourApplication app = TourApplication.builder()
                .tour(tour)
                .applicant(traveler)
                .status(TourApplicationStatus.PENDING)
                .message("Hello")
                .build();
        app = tourApplicationRepository.save(app);

        ReviewApplicationRequestDto reviewReq = new ReviewApplicationRequestDto(TourApplicationStatus.ACCEPTED, null);

        mockMvc.perform(post("/api/v1/tours/{tourId}/applications/{id}/review", tour.getId(), app.getId())
                        .header("Authorization", "Bearer " + organizerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
    }

    @Test
    @DisplayName("POST /api/v1/tours/{tourId}/applications/{id}/cancel - traveler cancels accepted application")
    void shouldCancelParticipationByTraveler() throws Exception {
        TourApplication app = TourApplication.builder()
                .tour(tour)
                .applicant(traveler)
                .status(TourApplicationStatus.ACCEPTED)
                .build();
        app = tourApplicationRepository.save(app);

        CancelApplicationRequestDto cancelReq = new CancelApplicationRequestDto("Personal reason");

        mockMvc.perform(post("/api/v1/tours/{tourId}/applications/{id}/cancel", tour.getId(), app.getId())
                        .header("Authorization", "Bearer " + travelerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CANCELLED_BY_TRAVELER"))
                .andExpect(jsonPath("$.data.cancellationReason").value("Personal reason"));
    }

    @Test
    @DisplayName("GET /api/v1/users/me/applications - traveler views their applications/trips")
    void shouldGetMyApplicationsForTraveler() throws Exception {
        TourApplication app = TourApplication.builder()
                .tour(tour)
                .applicant(traveler)
                .status(TourApplicationStatus.PENDING)
                .build();
        tourApplicationRepository.save(app);

        mockMvc.perform(get("/api/v1/users/me/applications")
                        .header("Authorization", "Bearer " + travelerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].tourTitle").value("Tian Shan Mountain Trek"));
    }
}
