package com.tourhive.backend.tours;

import com.tourhive.backend.dto.tours.request.AddTourImageRequestDto;
import com.tourhive.backend.dto.tours.request.CreateTourRequestDto;
import com.tourhive.backend.dto.tours.request.TourRouteItemRequestDto;
import com.tourhive.backend.dto.tours.request.UpdateTourRequestDto;
import com.tourhive.backend.dto.tours.response.TourImageResponseDto;
import com.tourhive.backend.dto.tours.response.TourResponseDto;
import com.tourhive.backend.entities.resources.Resource;
import com.tourhive.backend.entities.resources.ResourceType;
import com.tourhive.backend.entities.tours.Tour;
import com.tourhive.backend.entities.tours.TourImage;
import com.tourhive.backend.entities.tours.TourStatus;
import com.tourhive.backend.entities.tours.TourType;
import com.tourhive.backend.entities.users.Role;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserStatus;
import com.tourhive.backend.exceptions.CommercialTourNotAllowedException;
import com.tourhive.backend.exceptions.InvalidTourDataException;
import com.tourhive.backend.exceptions.TourAccessDeniedException;
import com.tourhive.backend.mappers.tours.TourMapper;
import com.tourhive.backend.repositories.resources.ResourceRepository;
import com.tourhive.backend.repositories.tours.TourImageRepository;
import com.tourhive.backend.repositories.tours.TourRepository;
import com.tourhive.backend.repositories.tours.TourRouteItemRepository;
import com.tourhive.backend.repositories.users.UserProfileRepository;
import com.tourhive.backend.repositories.users.UserRepository;
import com.tourhive.backend.services.tours.impl.TourServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TourServiceTest {

    @Mock
    private TourRepository tourRepository;

    @Mock
    private TourRouteItemRepository tourRouteItemRepository;

    @Mock
    private TourImageRepository tourImageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private ResourceRepository resourceRepository;

    @Spy
    private TourMapper tourMapper = Mappers.getMapper(TourMapper.class);

    @InjectMocks
    private TourServiceImpl tourService;

    private User organizerUser;
    private User businessOrganizerUser;
    private User regularUser;

    @BeforeEach
    void setUp() {
        organizerUser = User.builder()
                .email("organizer@example.com")
                .passwordHash("hash")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .isOrganizer(true)
                .isBusinessVerified(false)
                .build();
        organizerUser.setId(UUID.randomUUID());

        businessOrganizerUser = User.builder()
                .email("business@example.com")
                .passwordHash("hash")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .isOrganizer(true)
                .isBusinessVerified(true)
                .build();
        businessOrganizerUser.setId(UUID.randomUUID());

        regularUser = User.builder()
                .email("user@example.com")
                .passwordHash("hash")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .isOrganizer(false)
                .isBusinessVerified(false)
                .build();
        regularUser.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("createDraft should successfully create a COMMUNITY tour draft with route stops")
    void shouldCreateCommunityTourDraftSuccessfully() {
        when(userRepository.findById(organizerUser.getId())).thenReturn(Optional.of(organizerUser));
        when(tourRepository.save(any(Tour.class))).thenAnswer(invocation -> {
            Tour t = invocation.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        CreateTourRequestDto request = new CreateTourRequestDto(
                "Weekend in Charyn Canyon",
                "Scenic road trip with hiking",
                TourType.COMMUNITY,
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(7),
                10,
                BigDecimal.valueOf(25000),
                "KZT",
                List.of("Kazakhstan"),
                List.of("Almaty"),
                List.of("Транспорт", "Гид"),
                List.of(new TourRouteItemRequestDto("Almaty", LocalDate.now().plusDays(5), "Meeting", 0))
        );

        TourResponseDto response = tourService.createDraft(organizerUser.getId(), request);

        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("Weekend in Charyn Canyon");
        assertThat(response.tourType()).isEqualTo(TourType.COMMUNITY);
        assertThat(response.status()).isEqualTo(TourStatus.DRAFT);
        assertThat(response.capacity()).isEqualTo(10);
        assertThat(response.availablePlaces()).isEqualTo(10);
        assertThat(response.route()).hasSize(1);
        assertThat(response.includes()).containsExactly("Транспорт", "Гид");
        verify(tourRepository).save(any(Tour.class));
    }

    @Test
    @DisplayName("createDraft should fail when non-business-verified organizer attempts COMMERCIAL tour")
    void shouldFailCreatingCommercialTourIfNotBusinessVerified() {
        when(userRepository.findById(organizerUser.getId())).thenReturn(Optional.of(organizerUser));

        CreateTourRequestDto request = new CreateTourRequestDto(
                "Commercial Silk Road Tour",
                "Full VIP package",
                TourType.COMMERCIAL,
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(15),
                15,
                BigDecimal.valueOf(150000),
                "KZT",
                List.of("Kazakhstan"),
                List.of("Almaty"),
                null,
                null
        );

        assertThatThrownBy(() -> tourService.createDraft(organizerUser.getId(), request))
                .isInstanceOf(CommercialTourNotAllowedException.class)
                .hasMessageContaining("Commercial tours require verified business status");

        verify(tourRepository, never()).save(any());
    }

    @Test
    @DisplayName("createDraft should succeed for COMMERCIAL tour if organizer is business verified")
    void shouldCreateCommercialTourDraftIfBusinessVerified() {
        when(userRepository.findById(businessOrganizerUser.getId())).thenReturn(Optional.of(businessOrganizerUser));
        when(tourRepository.save(any(Tour.class))).thenAnswer(invocation -> {
            Tour t = invocation.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        CreateTourRequestDto request = new CreateTourRequestDto(
                "Commercial Silk Road Tour",
                "Full VIP package",
                TourType.COMMERCIAL,
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(15),
                15,
                BigDecimal.valueOf(150000),
                "KZT",
                List.of("Kazakhstan"),
                List.of("Almaty"),
                null,
                null
        );

        TourResponseDto response = tourService.createDraft(businessOrganizerUser.getId(), request);

        assertThat(response).isNotNull();
        assertThat(response.tourType()).isEqualTo(TourType.COMMERCIAL);
        assertThat(response.status()).isEqualTo(TourStatus.DRAFT);
    }

    @Test
    @DisplayName("createDraft should fail if user is not an organizer")
    void shouldFailIfNonOrganizerTriesToCreateTour() {
        when(userRepository.findById(regularUser.getId())).thenReturn(Optional.of(regularUser));

        CreateTourRequestDto request = new CreateTourRequestDto(
                "Trip", "Desc", TourType.COMMUNITY,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(2),
                5, BigDecimal.ZERO, "KZT", null, null, null, null
        );

        assertThatThrownBy(() -> tourService.createDraft(regularUser.getId(), request))
                .isInstanceOf(InvalidTourDataException.class)
                .hasMessageContaining("User must be an organizer");
    }

    @Test
    @DisplayName("createDraft should fail if end date is before start date")
    void shouldFailIfEndDateBeforeStartDate() {
        when(userRepository.findById(organizerUser.getId())).thenReturn(Optional.of(organizerUser));

        CreateTourRequestDto request = new CreateTourRequestDto(
                "Trip", "Desc", TourType.COMMUNITY,
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(5),
                5, BigDecimal.ZERO, "KZT", null, null, null, null
        );

        assertThatThrownBy(() -> tourService.createDraft(organizerUser.getId(), request))
                .isInstanceOf(InvalidTourDataException.class)
                .hasMessageContaining("end date must be after or equal to start date");
    }

    @Test
    @DisplayName("updateDraft should fail if non-owner tries to update tour")
    void shouldFailUpdatingDraftIfNotOwner() {
        Tour tour = Tour.builder()
                .organizer(organizerUser)
                .title("Original")
                .status(TourStatus.DRAFT)
                .tourType(TourType.COMMUNITY)
                .startDate(LocalDate.now().plusDays(5))
                .endDate(LocalDate.now().plusDays(10))
                .capacity(5)
                .build();
        tour.setId(UUID.randomUUID());

        when(tourRepository.findById(tour.getId())).thenReturn(Optional.of(tour));

        UpdateTourRequestDto updateRequest = new UpdateTourRequestDto(
                "Hacked Title", null, null, null, null, null, null, null, null, null, null, null
        );

        assertThatThrownBy(() -> tourService.updateDraft(tour.getId(), regularUser.getId(), updateRequest))
                .isInstanceOf(TourAccessDeniedException.class);
    }

    @Test
    @DisplayName("addTourImage should attach image and set as cover if first image")
    void shouldAddTourImageSuccessfully() {
        Tour tour = Tour.builder()
                .organizer(organizerUser)
                .title("Trip")
                .status(TourStatus.DRAFT)
                .tourType(TourType.COMMUNITY)
                .startDate(LocalDate.now().plusDays(5))
                .endDate(LocalDate.now().plusDays(10))
                .capacity(5)
                .images(new ArrayList<>())
                .build();
        tour.setId(UUID.randomUUID());

        UUID resourceId = UUID.randomUUID();
        Resource resource = new Resource("canyon.jpg", "tours/1.jpg", "tours", 500L, ResourceType.IMAGE, "image/jpeg");
        resource.setId(resourceId);

        when(tourRepository.findById(tour.getId())).thenReturn(Optional.of(tour));
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));
        when(tourImageRepository.save(any(TourImage.class))).thenAnswer(invocation -> {
            TourImage img = invocation.getArgument(0);
            img.setId(UUID.randomUUID());
            return img;
        });

        TourImageResponseDto imageDto = tourService.addTourImage(
                tour.getId(),
                organizerUser.getId(),
                new AddTourImageRequestDto(resourceId, false, 0)
        );

        assertThat(imageDto).isNotNull();
        assertThat(imageDto.resourceId()).isEqualTo(resourceId);
        assertThat(imageDto.url()).isEqualTo("/api/v1/resources/" + resourceId);
        assertThat(imageDto.isCover()).isTrue();
    }
}
