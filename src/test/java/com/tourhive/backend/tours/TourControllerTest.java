package com.tourhive.backend.tours;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourhive.backend.configurations.security.JwtTokenProvider;
import com.tourhive.backend.dto.tours.request.AddTourImageRequestDto;
import com.tourhive.backend.dto.tours.request.CreateTourRequestDto;
import com.tourhive.backend.dto.tours.request.TourRouteItemRequestDto;
import com.tourhive.backend.dto.tours.request.UpdateTourRequestDto;
import com.tourhive.backend.entities.resources.Resource;
import com.tourhive.backend.entities.resources.ResourceType;
import com.tourhive.backend.entities.tours.Tour;
import com.tourhive.backend.entities.tours.TourStatus;
import com.tourhive.backend.entities.tours.TourType;
import com.tourhive.backend.entities.users.Role;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserStatus;
import com.tourhive.backend.repositories.resources.ResourceRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TourControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TourRepository tourRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private DatabaseCleanup databaseCleanup;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User organizerUser;
    private String organizerToken;

    private User businessUser;
    private String businessToken;

    private User otherUser;
    private String otherToken;

    @BeforeEach
    void setUp() {
        databaseCleanup.clean();

        organizerUser = userRepository.save(User.builder()
                .email("organizer@tourhive.com")
                .passwordHash("hash")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .isOrganizer(true)
                .isBusinessVerified(false)
                .build());
        organizerToken = jwtTokenProvider.generateAccessToken(organizerUser);

        businessUser = userRepository.save(User.builder()
                .email("business@tourhive.com")
                .passwordHash("hash")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .isOrganizer(true)
                .isBusinessVerified(true)
                .build());
        businessToken = jwtTokenProvider.generateAccessToken(businessUser);

        otherUser = userRepository.save(User.builder()
                .email("other@tourhive.com")
                .passwordHash("hash")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .isOrganizer(false)
                .isBusinessVerified(false)
                .build());
        otherToken = jwtTokenProvider.generateAccessToken(otherUser);
    }

    @Test
    @DisplayName("POST /api/v1/tours/draft should create COMMUNITY draft")
    void shouldCreateCommunityDraft() throws Exception {
        CreateTourRequestDto request = new CreateTourRequestDto(
                "Kolsay and Kaindy Lakes",
                "Two-day weekend exploration",
                TourType.COMMUNITY,
                LocalDate.now().plusDays(3),
                LocalDate.now().plusDays(5),
                8,
                BigDecimal.valueOf(35000),
                "KZT",
                List.of("Kazakhstan"),
                List.of("Almaty"),
                List.of("Транспорт", "Питание"),
                List.of(new TourRouteItemRequestDto("Saty village", LocalDate.now().plusDays(3), "Arrival and dinner", 0))
        );

        mockMvc.perform(post("/api/v1/tours/draft")
                        .header("Authorization", "Bearer " + organizerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Kolsay and Kaindy Lakes"))
                .andExpect(jsonPath("$.data.tourType").value("COMMUNITY"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.capacity").value(8))
                .andExpect(jsonPath("$.data.availablePlaces").value(8))
                .andExpect(jsonPath("$.data.route", hasSize(1)));
    }

    @Test
    @DisplayName("POST /api/v1/tours/draft should forbid COMMERCIAL draft if organizer not business verified")
    void shouldRejectCommercialDraftForUnverifiedOrganizer() throws Exception {
        CreateTourRequestDto request = new CreateTourRequestDto(
                "Luxury Pamir Expedition",
                "High altitude trip",
                TourType.COMMERCIAL,
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(20),
                12,
                BigDecimal.valueOf(500000),
                "KZT",
                List.of("Tajikistan"),
                List.of("Dushanbe"),
                List.of("Отель"),
                null
        );

        mockMvc.perform(post("/api/v1/tours/draft")
                        .header("Authorization", "Bearer " + organizerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMMERCIAL_TOUR_NOT_ALLOWED"));
    }

    @Test
    @DisplayName("POST /api/v1/tours/draft should allow COMMERCIAL draft if organizer is business verified")
    void shouldAllowCommercialDraftForVerifiedBusiness() throws Exception {
        CreateTourRequestDto request = new CreateTourRequestDto(
                "Luxury Pamir Expedition",
                "High altitude trip",
                TourType.COMMERCIAL,
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(20),
                12,
                BigDecimal.valueOf(500000),
                "KZT",
                List.of("Tajikistan"),
                List.of("Dushanbe"),
                List.of("Отель"),
                null
        );

        mockMvc.perform(post("/api/v1/tours/draft")
                        .header("Authorization", "Bearer " + businessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.tourType").value("COMMERCIAL"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    @DisplayName("GET /api/v1/tours/my-drafts should return organizer's drafts")
    void shouldReturnMyDrafts() throws Exception {
        tourRepository.save(Tour.builder()
                .organizer(organizerUser)
                .title("Draft 1")
                .tourType(TourType.COMMUNITY)
                .status(TourStatus.DRAFT)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(2))
                .capacity(5)
                .build());

        mockMvc.perform(get("/api/v1/tours/my-drafts")
                        .header("Authorization", "Bearer " + organizerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].title").value("Draft 1"));
    }

    @Test
    @DisplayName("GET /api/v1/tours/{id} should forbid access to draft by another user")
    void shouldForbidAccessToDraftByOtherUser() throws Exception {
        Tour tour = tourRepository.save(Tour.builder()
                .organizer(organizerUser)
                .title("Secret Draft")
                .tourType(TourType.COMMUNITY)
                .status(TourStatus.DRAFT)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(2))
                .capacity(5)
                .build());

        mockMvc.perform(get("/api/v1/tours/" + tour.getId())
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_TOUR_ACCESS"));
    }

    @Test
    @DisplayName("PUT /api/v1/tours/{id} should update draft fields")
    void shouldUpdateDraft() throws Exception {
        Tour tour = tourRepository.save(Tour.builder()
                .organizer(organizerUser)
                .title("Old Title")
                .tourType(TourType.COMMUNITY)
                .status(TourStatus.DRAFT)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(2))
                .capacity(5)
                .build());

        UpdateTourRequestDto updateRequest = new UpdateTourRequestDto(
                "Updated Title", "Updated Description", null, null, null, 10, null, null, null, null, null, null
        );

        mockMvc.perform(put("/api/v1/tours/" + tour.getId())
                        .header("Authorization", "Bearer " + organizerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated Title"))
                .andExpect(jsonPath("$.data.capacity").value(10))
                .andExpect(jsonPath("$.data.availablePlaces").value(10));
    }

    @Test
    @DisplayName("POST /api/v1/tours/{id}/images should attach uploaded resource")
    void shouldAttachImageToTour() throws Exception {
        Tour tour = tourRepository.save(Tour.builder()
                .organizer(organizerUser)
                .title("Trip")
                .tourType(TourType.COMMUNITY)
                .status(TourStatus.DRAFT)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(2))
                .capacity(5)
                .build());

        Resource resource = resourceRepository.save(new Resource(
                "lake.jpg", "tours/lake.jpg", "tours", 2048L, ResourceType.IMAGE, "image/jpeg"
        ));

        AddTourImageRequestDto imageRequest = new AddTourImageRequestDto(resource.getId(), true, 0);

        mockMvc.perform(post("/api/v1/tours/" + tour.getId() + "/images")
                        .header("Authorization", "Bearer " + organizerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(imageRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.resourceId").value(resource.getId().toString()))
                .andExpect(jsonPath("$.data.isCover").value(true))
                .andExpect(jsonPath("$.data.url").value("/api/v1/resources/" + resource.getId()));
    }

    @Test
    @DisplayName("DELETE /api/v1/tours/{id} should delete draft tour")
    void shouldDeleteDraft() throws Exception {
        Tour tour = tourRepository.save(Tour.builder()
                .organizer(organizerUser)
                .title("Trip to delete")
                .tourType(TourType.COMMUNITY)
                .status(TourStatus.DRAFT)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(2))
                .capacity(5)
                .build());

        mockMvc.perform(delete("/api/v1/tours/" + tour.getId())
                        .header("Authorization", "Bearer " + organizerToken))
                .andExpect(status().isOk());

        assertThat(tourRepository.findById(tour.getId())).isEmpty();
    }
}
