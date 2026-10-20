package com.tourhive.backend.services.tours.impl;

import com.tourhive.backend.dto.tours.request.AddTourImageRequestDto;
import com.tourhive.backend.dto.tours.request.CancelTourRequestDto;
import com.tourhive.backend.dto.tours.request.CreateTourRequestDto;
import com.tourhive.backend.dto.tours.request.TourRouteItemRequestDto;
import com.tourhive.backend.dto.tours.request.TourSearchCriteria;
import com.tourhive.backend.dto.tours.request.UpdateTourRequestDto;
import com.tourhive.backend.dto.tours.response.TourImageResponseDto;
import com.tourhive.backend.dto.tours.response.TourResponseDto;
import com.tourhive.backend.dto.tours.response.TourShortResponseDto;
import com.tourhive.backend.spec.tours.TourSpecification;
import com.tourhive.backend.spec.tours.builder.TourSpecificationBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.tourhive.backend.entities.resources.Resource;
import com.tourhive.backend.entities.tours.Tour;
import com.tourhive.backend.entities.tours.TourImage;
import com.tourhive.backend.entities.tours.TourRouteItem;
import com.tourhive.backend.entities.tours.ApplicationStatusHistory;
import com.tourhive.backend.entities.tours.TourApplication;
import com.tourhive.backend.entities.tours.TourApplicationStatus;
import com.tourhive.backend.entities.tours.TourStatus;
import com.tourhive.backend.entities.tours.TourType;
import com.tourhive.backend.entities.users.Role;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserProfile;
import com.tourhive.backend.exceptions.CommercialTourNotAllowedException;
import com.tourhive.backend.exceptions.InvalidTourDataException;
import com.tourhive.backend.exceptions.ResourceNotFoundException;
import com.tourhive.backend.exceptions.TourAccessDeniedException;
import com.tourhive.backend.exceptions.TourNotFoundException;
import com.tourhive.backend.mappers.tours.TourMapper;
import com.tourhive.backend.repositories.resources.ResourceRepository;
import com.tourhive.backend.repositories.tours.ApplicationStatusHistoryRepository;
import com.tourhive.backend.repositories.tours.TourApplicationRepository;
import com.tourhive.backend.repositories.tours.TourImageRepository;
import com.tourhive.backend.repositories.tours.TourRepository;
import com.tourhive.backend.repositories.tours.TourRouteItemRepository;
import com.tourhive.backend.repositories.users.UserProfileRepository;
import com.tourhive.backend.repositories.users.UserRepository;
import com.tourhive.backend.services.tours.TourService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class TourServiceImpl implements TourService {

    private final TourRepository tourRepository;
    private final TourRouteItemRepository tourRouteItemRepository;
    private final TourImageRepository tourImageRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final ResourceRepository resourceRepository;
    private final TourMapper tourMapper;
    private final TourApplicationRepository tourApplicationRepository;
    private final ApplicationStatusHistoryRepository applicationStatusHistoryRepository;
    private final com.tourhive.backend.services.chats.ChatService chatService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public TourServiceImpl(
            TourRepository tourRepository,
            TourRouteItemRepository tourRouteItemRepository,
            TourImageRepository tourImageRepository,
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            ResourceRepository resourceRepository,
            TourMapper tourMapper,
            TourApplicationRepository tourApplicationRepository,
            ApplicationStatusHistoryRepository applicationStatusHistoryRepository,
            com.tourhive.backend.services.chats.ChatService chatService,
            org.springframework.context.ApplicationEventPublisher eventPublisher
    ) {
        this.tourRepository = tourRepository;
        this.tourRouteItemRepository = tourRouteItemRepository;
        this.tourImageRepository = tourImageRepository;
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.resourceRepository = resourceRepository;
        this.tourMapper = tourMapper;
        this.tourApplicationRepository = tourApplicationRepository;
        this.applicationStatusHistoryRepository = applicationStatusHistoryRepository;
        this.chatService = chatService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public TourResponseDto createDraft(UUID organizerId, CreateTourRequestDto requestDto) {
        User organizer = getActiveOrganizer(organizerId);

        if (requestDto.tourType() == TourType.COMMERCIAL && !organizer.isBusinessVerified()) {
            throw new CommercialTourNotAllowedException("Commercial tours require verified business status");
        }

        validateTourDates(requestDto.startDate(), requestDto.endDate());

        Tour tour = Tour.builder()
                .organizer(organizer)
                .title(requestDto.title())
                .description(requestDto.description())
                .tourType(requestDto.tourType())
                .status(TourStatus.DRAFT)
                .startDate(requestDto.startDate())
                .endDate(requestDto.endDate())
                .capacity(requestDto.capacity())
                .availablePlaces(requestDto.capacity())
                .price(requestDto.price())
                .currency(requestDto.currency() != null ? requestDto.currency() : "KZT")
                .countries(requestDto.countries() != null ? new ArrayList<>(requestDto.countries()) : new ArrayList<>())
                .cities(requestDto.cities() != null ? new ArrayList<>(requestDto.cities()) : new ArrayList<>())
                .includes(requestDto.includes() != null ? new ArrayList<>(requestDto.includes()) : new ArrayList<>())
                .build();

        if (requestDto.route() != null && !requestDto.route().isEmpty()) {
            int index = 0;
            for (TourRouteItemRequestDto routeDto : requestDto.route()) {
                TourRouteItem item = TourRouteItem.builder()
                        .city(routeDto.city())
                        .routeDate(routeDto.routeDate())
                        .description(routeDto.description())
                        .orderIndex(routeDto.orderIndex() != null ? routeDto.orderIndex() : index++)
                        .build();
                tour.addRouteItem(item);
            }
        }

        Tour saved = tourRepository.save(tour);
        log.info("Created tour draft with id: {} for organizer: {}", saved.getId(), organizerId);
        UserProfile profile = userProfileRepository.findByUserId(organizerId).orElse(null);
        return tourMapper.toResponseDto(saved, profile);
    }

    @Override
    @Transactional
    public TourResponseDto updateDraft(UUID tourId, UUID organizerId, UpdateTourRequestDto requestDto) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new TourNotFoundException("Tour not found: " + tourId));

        validateOwnership(tour, organizerId);

        if (tour.getStatus() != TourStatus.DRAFT && tour.getStatus() != TourStatus.REJECTED) {
            throw new InvalidTourDataException("Only draft or rejected tours can be modified directly");
        }

        TourType targetType = requestDto.tourType() != null ? requestDto.tourType() : tour.getTourType();
        if (targetType == TourType.COMMERCIAL && !tour.getOrganizer().isBusinessVerified()) {
            throw new CommercialTourNotAllowedException("Commercial tours require verified business status");
        }
        tour.setTourType(targetType);

        LocalDate startDate = requestDto.startDate() != null ? requestDto.startDate() : tour.getStartDate();
        LocalDate endDate = requestDto.endDate() != null ? requestDto.endDate() : tour.getEndDate();
        validateTourDates(startDate, endDate);
        tour.setStartDate(startDate);
        tour.setEndDate(endDate);

        if (requestDto.title() != null) {
            tour.setTitle(requestDto.title());
        }
        if (requestDto.description() != null) {
            tour.setDescription(requestDto.description());
        }
        if (requestDto.capacity() != null) {
            tour.setCapacity(requestDto.capacity());
            tour.setAvailablePlaces(requestDto.capacity());
        }
        if (requestDto.price() != null) {
            tour.setPrice(requestDto.price());
        }
        if (requestDto.currency() != null) {
            tour.setCurrency(requestDto.currency());
        }
        if (requestDto.countries() != null) {
            tour.getCountries().clear();
            tour.getCountries().addAll(requestDto.countries());
        }
        if (requestDto.cities() != null) {
            tour.getCities().clear();
            tour.getCities().addAll(requestDto.cities());
        }
        if (requestDto.includes() != null) {
            tour.getIncludes().clear();
            tour.getIncludes().addAll(requestDto.includes());
        }

        if (requestDto.route() != null) {
            tour.getRouteItems().clear();
            int index = 0;
            for (TourRouteItemRequestDto routeDto : requestDto.route()) {
                TourRouteItem item = TourRouteItem.builder()
                        .city(routeDto.city())
                        .routeDate(routeDto.routeDate())
                        .description(routeDto.description())
                        .orderIndex(routeDto.orderIndex() != null ? routeDto.orderIndex() : index++)
                        .build();
                tour.addRouteItem(item);
            }
        }

        Tour saved = tourRepository.save(tour);
        log.info("Updated tour draft id: {} by organizer: {}", saved.getId(), organizerId);
        UserProfile profile = userProfileRepository.findByUserId(organizerId).orElse(null);
        return tourMapper.toResponseDto(saved, profile);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TourShortResponseDto> getMyDrafts(UUID organizerId) {
        List<Tour> drafts = tourRepository.findAllByOrganizerIdAndStatusOrderByCreatedAtDesc(organizerId, TourStatus.DRAFT);
        UserProfile profile = userProfileRepository.findByUserId(organizerId).orElse(null);
        return drafts.stream()
                .map(t -> tourMapper.toShortResponseDto(t, profile))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TourResponseDto getTourById(UUID tourId, UUID currentUserId) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new TourNotFoundException("Tour not found: " + tourId));

        if (tour.getStatus() != TourStatus.PUBLISHED && tour.getStatus() != TourStatus.COMPLETED) {
            if (currentUserId == null) {
                throw new TourAccessDeniedException("Non-published tours can only be viewed by their organizer");
            }
            User currentUser = userRepository.findById(currentUserId).orElse(null);
            boolean isOwner = tour.getOrganizer().getId().equals(currentUserId);
            boolean isModerator = currentUser != null && (currentUser.getRole() == Role.MODERATOR || currentUser.getRole() == Role.ADMIN);

            if (!isOwner && !isModerator) {
                throw new TourAccessDeniedException("Non-published tours can only be viewed by their organizer");
            }
        }

        UserProfile profile = userProfileRepository.findByUserId(tour.getOrganizer().getId()).orElse(null);
        return tourMapper.toResponseDto(tour, profile);
    }

    @Override
    @Transactional
    public void deleteTour(UUID tourId, UUID organizerId) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new TourNotFoundException("Tour not found: " + tourId));

        validateOwnership(tour, organizerId);

        if (tour.getStatus() != TourStatus.DRAFT && tour.getStatus() != TourStatus.REJECTED) {
            throw new InvalidTourDataException("Only draft or rejected tours can be deleted");
        }

        tourRepository.delete(tour);
        log.info("Deleted tour id: {} by organizer: {}", tourId, organizerId);
    }

    @Override
    @Transactional
    public TourImageResponseDto addTourImage(UUID tourId, UUID organizerId, AddTourImageRequestDto requestDto) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new TourNotFoundException("Tour not found: " + tourId));

        validateOwnership(tour, organizerId);

        Resource resource = resourceRepository.findById(requestDto.resourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + requestDto.resourceId()));

        boolean isCover = Boolean.TRUE.equals(requestDto.isCover());
        if (isCover) {
            for (TourImage existing : tour.getImages()) {
                existing.setIsCover(false);
            }
        } else if (tour.getImages().isEmpty()) {
            isCover = true;
        }

        int nextIndex = requestDto.orderIndex() != null ? requestDto.orderIndex() : tour.getImages().size();
        TourImage image = TourImage.builder()
                .tour(tour)
                .resource(resource)
                .isCover(isCover)
                .orderIndex(nextIndex)
                .build();

        tour.addImage(image);
        TourImage savedImage = tourImageRepository.save(image);
        log.info("Attached image resource {} to tour {}", resource.getId(), tourId);
        return tourMapper.toImageDto(savedImage);
    }

    @Override
    @Transactional
    public void removeTourImage(UUID tourId, UUID imageId, UUID organizerId) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new TourNotFoundException("Tour not found: " + tourId));

        validateOwnership(tour, organizerId);

        TourImage image = tourImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Tour image not found: " + imageId));

        if (!image.getTour().getId().equals(tourId)) {
            throw new InvalidTourDataException("Image does not belong to tour: " + tourId);
        }

        tour.removeImage(image);
        tourImageRepository.delete(image);
        log.info("Removed image {} from tour {}", imageId, tourId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TourShortResponseDto> searchPublishedTours(TourSearchCriteria criteria, Pageable pageable) {
        if (criteria != null) {
            if (criteria.dateFrom() != null && criteria.dateTo() != null && criteria.dateFrom().isAfter(criteria.dateTo())) {
                throw new InvalidTourDataException("dateFrom must be before or equal to dateTo");
            }
            if (criteria.minPrice() != null && criteria.maxPrice() != null && criteria.minPrice().compareTo(criteria.maxPrice()) > 0) {
                throw new InvalidTourDataException("minPrice must be less than or equal to maxPrice");
            }
        }

        var builder = new TourSpecificationBuilder();
        if (criteria != null) {
            builder.withSearch(criteria.search())
                    .withCountry(criteria.country())
                    .withCity(criteria.city())
                    .withTourType(criteria.tourType())
                    .withDateFrom(criteria.dateFrom())
                    .withDateTo(criteria.dateTo())
                    .withMinPrice(criteria.minPrice())
                    .withMaxPrice(criteria.maxPrice())
                    .withHasAvailablePlaces(criteria.hasAvailablePlaces());
        }
        var spec = builder.build();

        // Mandatory visibility restriction: only PUBLISHED tours in public catalog
        spec = spec.and(TourSpecification.hasStatus(TourStatus.PUBLISHED));

        Page<Tour> page = tourRepository.findAll(spec, pageable);
        return page.map(tour -> {
            UserProfile profile = userProfileRepository.findByUserId(tour.getOrganizer().getId()).orElse(null);
            return tourMapper.toShortResponseDto(tour, profile);
        });
    }

    @Override
    @Transactional
    public TourResponseDto publishTour(UUID tourId, UUID organizerId) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new TourNotFoundException("Tour not found: " + tourId));

        validateOwnership(tour, organizerId);

        if (tour.getStatus() != TourStatus.DRAFT && tour.getStatus() != TourStatus.REJECTED) {
            throw new InvalidTourDataException("Only draft or rejected tours can be submitted for publication");
        }

        if (tour.getTitle() == null || tour.getTitle().isBlank()) {
            throw new InvalidTourDataException("Tour title is required for publication");
        }
        if (tour.getStartDate() == null || tour.getEndDate() == null) {
            throw new InvalidTourDataException("Tour start and end dates are required for publication");
        }
        validateTourDates(tour.getStartDate(), tour.getEndDate());

        if (tour.getCapacity() == null || tour.getCapacity() < 1) {
            throw new InvalidTourDataException("Tour capacity must be at least 1");
        }
        if (tour.getCountries().isEmpty() && tour.getCities().isEmpty()) {
            throw new InvalidTourDataException("At least one destination country or city is required for publication");
        }
        if (tour.getImages().isEmpty()) {
            throw new InvalidTourDataException("At least one tour photo is required for publication");
        }

        if (tour.getTourType() == TourType.COMMERCIAL && !tour.getOrganizer().isBusinessVerified()) {
            throw new CommercialTourNotAllowedException("Commercial tours require verified business status");
        }

        tour.setStatus(TourStatus.PENDING_REVIEW);
        tour.setRejectionReason(null);
        Tour saved = tourRepository.save(tour);
        log.info("Tour {} submitted for moderation by organizer {}", saved.getId(), organizerId);

        UserProfile profile = userProfileRepository.findByUserId(organizerId).orElse(null);
        return tourMapper.toResponseDto(saved, profile);
    }

    @Override
    @Transactional
    public TourResponseDto cancelTour(UUID tourId, UUID organizerId, CancelTourRequestDto requestDto) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new TourNotFoundException("Tour not found: " + tourId));

        validateOwnership(tour, organizerId);

        if (tour.getStatus() != TourStatus.PUBLISHED && tour.getStatus() != TourStatus.PENDING_REVIEW) {
            throw new InvalidTourDataException("Only published or pending review tours can be cancelled");
        }

        tour.setStatus(TourStatus.CANCELLED);
        tour.setCancellationReason(requestDto.reason());
        Tour saved = tourRepository.save(tour);
        log.info("Tour {} cancelled by organizer {} with reason: {}", saved.getId(), organizerId, requestDto.reason());

        // Invariant: cascade cancellation to active applications (PENDING and ACCEPTED)
        List<TourApplication> activeApps = tourApplicationRepository.findAllByTourIdAndStatusIn(
                tourId,
                List.of(TourApplicationStatus.PENDING, TourApplicationStatus.ACCEPTED)
        );
        for (TourApplication app : activeApps) {
            TourApplicationStatus old = app.getStatus();
            app.setStatus(TourApplicationStatus.CANCELLED_BY_ORGANIZER);
            app.setCancellationReason(requestDto.reason());
            tourApplicationRepository.save(app);

            ApplicationStatusHistory history = ApplicationStatusHistory.builder()
                    .application(app)
                    .oldStatus(old)
                    .newStatus(TourApplicationStatus.CANCELLED_BY_ORGANIZER)
                    .changedBy(tour.getOrganizer())
                    .reason("Tour was cancelled by organizer: " + requestDto.reason())
                    .build();
            applicationStatusHistoryRepository.save(history);

            eventPublisher.publishEvent(new com.tourhive.backend.events.NotificationEvent(
                    app.getApplicant().getId(),
                    com.tourhive.backend.entities.notifications.NotificationType.TOUR_CANCELLED,
                    "Tour Cancelled",
                    "Tour '" + tour.getTitle() + "' has been cancelled by the organizer.",
                    "TOUR",
                    tour.getId()
            ));
        }

        try {
            chatService.deactivateChatForCancelledTour(tourId);
        } catch (Exception e) {
            log.warn("Failed to deactivate tour chat on cancel", e);
        }

        UserProfile profile = userProfileRepository.findByUserId(organizerId).orElse(null);
        return tourMapper.toResponseDto(saved, profile);
    }

    private User getActiveOrganizer(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (!user.isOrganizer()) {
            throw new InvalidTourDataException("User must be an organizer to create tours");
        }
        return user;
    }

    private void validateOwnership(Tour tour, UUID organizerId) {
        if (!tour.getOrganizer().getId().equals(organizerId)) {
            throw new TourAccessDeniedException("You do not have permission to modify this tour");
        }
    }

    private void validateTourDates(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new InvalidTourDataException("Tour end date must be after or equal to start date");
        }
    }
}
