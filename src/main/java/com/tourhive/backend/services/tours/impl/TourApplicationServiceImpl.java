package com.tourhive.backend.services.tours.impl;

import com.tourhive.backend.dto.tours.request.ApplyTourRequestDto;
import com.tourhive.backend.dto.tours.request.CancelApplicationRequestDto;
import com.tourhive.backend.dto.tours.request.ReviewApplicationRequestDto;
import com.tourhive.backend.dto.tours.response.TourApplicationResponseDto;
import com.tourhive.backend.dto.tours.response.TourApplicationShortResponseDto;
import com.tourhive.backend.entities.tours.ApplicationStatusHistory;
import com.tourhive.backend.entities.tours.Tour;
import com.tourhive.backend.entities.tours.TourApplication;
import com.tourhive.backend.entities.tours.TourApplicationStatus;
import com.tourhive.backend.entities.tours.TourStatus;
import com.tourhive.backend.entities.users.Role;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserProfile;
import com.tourhive.backend.exceptions.ApplicationAccessDeniedException;
import com.tourhive.backend.exceptions.ApplicationNotFoundException;
import com.tourhive.backend.exceptions.DuplicateApplicationException;
import com.tourhive.backend.exceptions.InvalidApplicationStateException;
import com.tourhive.backend.exceptions.ResourceNotFoundException;
import com.tourhive.backend.exceptions.TourCapacityExceededException;
import com.tourhive.backend.exceptions.TourNotFoundException;
import com.tourhive.backend.mappers.tours.TourApplicationMapper;
import com.tourhive.backend.repositories.tours.ApplicationStatusHistoryRepository;
import com.tourhive.backend.repositories.tours.TourApplicationRepository;
import com.tourhive.backend.repositories.tours.TourRepository;
import com.tourhive.backend.repositories.users.UserProfileRepository;
import com.tourhive.backend.repositories.users.UserRepository;
import com.tourhive.backend.services.tours.TourApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@Transactional(readOnly = true)
public class TourApplicationServiceImpl implements TourApplicationService {

    private static final Set<TourApplicationStatus> ACTIVE_STATUSES = Set.of(
            TourApplicationStatus.PENDING,
            TourApplicationStatus.ACCEPTED
    );

    private final TourApplicationRepository tourApplicationRepository;
    private final ApplicationStatusHistoryRepository applicationStatusHistoryRepository;
    private final TourRepository tourRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final TourApplicationMapper tourApplicationMapper;
    private final com.tourhive.backend.services.chats.ChatService chatService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public TourApplicationServiceImpl(
            TourApplicationRepository tourApplicationRepository,
            ApplicationStatusHistoryRepository applicationStatusHistoryRepository,
            TourRepository tourRepository,
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            TourApplicationMapper tourApplicationMapper,
            com.tourhive.backend.services.chats.ChatService chatService,
            org.springframework.context.ApplicationEventPublisher eventPublisher
    ) {
        this.tourApplicationRepository = tourApplicationRepository;
        this.applicationStatusHistoryRepository = applicationStatusHistoryRepository;
        this.tourRepository = tourRepository;
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.tourApplicationMapper = tourApplicationMapper;
        this.chatService = chatService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public TourApplicationResponseDto applyForTour(UUID tourId, UUID applicantId, ApplyTourRequestDto requestDto) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new TourNotFoundException("Tour not found: " + tourId));

        if (tour.getStatus() != TourStatus.PUBLISHED) {
            throw new InvalidApplicationStateException("Applications can only be submitted for published tours");
        }

        if (tour.getStartDate().isBefore(LocalDate.now())) {
            throw new InvalidApplicationStateException("Cannot apply for a tour that has already started or ended");
        }

        if (tour.getOrganizer().getId().equals(applicantId)) {
            throw new InvalidApplicationStateException("Organizers cannot apply to their own tours");
        }

        User applicant = userRepository.findById(applicantId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + applicantId));

        boolean alreadyActive = tourApplicationRepository.existsByTourIdAndApplicantIdAndStatusIn(tourId, applicantId, ACTIVE_STATUSES);
        if (alreadyActive) {
            throw new DuplicateApplicationException("You already have an active application for this tour");
        }

        TourApplication application = TourApplication.builder()
                .tour(tour)
                .applicant(applicant)
                .status(TourApplicationStatus.PENDING)
                .message(requestDto != null ? requestDto.message() : null)
                .contactInfo(requestDto != null ? requestDto.contactInfo() : null)
                .build();

        TourApplication saved = tourApplicationRepository.save(application);

        ApplicationStatusHistory history = ApplicationStatusHistory.builder()
                .application(saved)
                .oldStatus(null)
                .newStatus(TourApplicationStatus.PENDING)
                .changedBy(applicant)
                .reason("Application submitted by traveler")
                .build();
        applicationStatusHistoryRepository.save(history);

        log.info("User {} applied for tour {}, application id: {}", applicantId, tourId, saved.getId());

        eventPublisher.publishEvent(new com.tourhive.backend.events.NotificationEvent(
                tour.getOrganizer().getId(),
                com.tourhive.backend.entities.notifications.NotificationType.APPLICATION_RECEIVED,
                "New Tour Application",
                "A traveler applied for your tour '" + tour.getTitle() + "'",
                "APPLICATION",
                saved.getId()
        ));

        UserProfile profile = userProfileRepository.findByUserId(applicantId).orElse(null);
        return tourApplicationMapper.toResponseDto(saved, profile, List.of(history));
    }

    @Override
    @Transactional
    public TourApplicationResponseDto reviewApplication(UUID tourId, UUID applicationId, UUID organizerId, ReviewApplicationRequestDto requestDto) {
        // Lock tour row for update to ensure atomic capacity checks and prevent overbooking
        Tour tour = tourRepository.findByIdWithPessimisticLock(tourId)
                .orElseThrow(() -> new TourNotFoundException("Tour not found: " + tourId));

        if (!tour.getOrganizer().getId().equals(organizerId)) {
            throw new ApplicationAccessDeniedException("Only the tour organizer can review applications");
        }

        TourApplication application = tourApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException("Application not found: " + applicationId));

        if (!application.getTour().getId().equals(tourId)) {
            throw new ApplicationNotFoundException("Application does not belong to tour: " + tourId);
        }

        if (application.getStatus() != TourApplicationStatus.PENDING) {
            throw new InvalidApplicationStateException("Only PENDING applications can be reviewed");
        }

        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + organizerId));

        TourApplicationStatus oldStatus = application.getStatus();
        TourApplicationStatus targetStatus = requestDto.status();

        if (targetStatus == TourApplicationStatus.ACCEPTED) {
            if (tour.getAvailablePlaces() <= 0) {
                throw new TourCapacityExceededException("No available places left in this tour");
            }
            tour.setAvailablePlaces(tour.getAvailablePlaces() - 1);
            tourRepository.save(tour);

            application.setStatus(TourApplicationStatus.ACCEPTED);
            application.setRejectionReason(null);
        } else if (targetStatus == TourApplicationStatus.REJECTED) {
            application.setStatus(TourApplicationStatus.REJECTED);
            application.setRejectionReason(requestDto.rejectionReason());
        } else {
            throw new InvalidApplicationStateException("Target review status must be either ACCEPTED or REJECTED");
        }

        TourApplication saved = tourApplicationRepository.save(application);

        ApplicationStatusHistory history = ApplicationStatusHistory.builder()
                .application(saved)
                .oldStatus(oldStatus)
                .newStatus(targetStatus)
                .changedBy(organizer)
                .reason(requestDto.rejectionReason())
                .build();
        applicationStatusHistoryRepository.save(history);

        log.info("Application {} for tour {} reviewed by organizer {}: status={}", applicationId, tourId, organizerId, targetStatus);
        if (targetStatus == TourApplicationStatus.ACCEPTED) {
            try {
                chatService.addTourParticipantToChat(tourId, saved.getApplicant().getId());
            } catch (Exception e) {
                log.warn("Failed to add participant to tour chat", e);
            }
            eventPublisher.publishEvent(new com.tourhive.backend.events.NotificationEvent(
                    saved.getApplicant().getId(),
                    com.tourhive.backend.entities.notifications.NotificationType.APPLICATION_ACCEPTED,
                    "Application Accepted",
                    "Your application for tour '" + tour.getTitle() + "' has been accepted!",
                    "APPLICATION",
                    saved.getId()
            ));
        } else if (targetStatus == TourApplicationStatus.REJECTED) {
            eventPublisher.publishEvent(new com.tourhive.backend.events.NotificationEvent(
                    saved.getApplicant().getId(),
                    com.tourhive.backend.entities.notifications.NotificationType.APPLICATION_REJECTED,
                    "Application Rejected",
                    "Your application for tour '" + tour.getTitle() + "' was not accepted." +
                            (requestDto.rejectionReason() != null ? " Reason: " + requestDto.rejectionReason() : ""),
                    "APPLICATION",
                    saved.getId()
            ));
        }
        UserProfile profile = userProfileRepository.findByUserId(saved.getApplicant().getId()).orElse(null);
        List<ApplicationStatusHistory> historyList = applicationStatusHistoryRepository.findAllByApplicationIdOrderByCreatedAtAsc(applicationId);
        return tourApplicationMapper.toResponseDto(saved, profile, historyList);
    }

    @Override
    @Transactional
    public TourApplicationResponseDto cancelApplication(UUID tourId, UUID applicationId, UUID currentUserId, CancelApplicationRequestDto requestDto) {
        // Lock tour row for update to ensure atomic capacity restore
        Tour tour = tourRepository.findByIdWithPessimisticLock(tourId)
                .orElseThrow(() -> new TourNotFoundException("Tour not found: " + tourId));

        TourApplication application = tourApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException("Application not found: " + applicationId));

        if (!application.getTour().getId().equals(tourId)) {
            throw new ApplicationNotFoundException("Application does not belong to tour: " + tourId);
        }

        if (!application.getApplicant().getId().equals(currentUserId)) {
            throw new ApplicationAccessDeniedException("Only the applicant can withdraw or cancel their application");
        }

        User applicant = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUserId));

        TourApplicationStatus oldStatus = application.getStatus();
        TourApplicationStatus targetStatus;
        String reason = requestDto != null ? requestDto.reason() : null;

        if (oldStatus == TourApplicationStatus.PENDING) {
            targetStatus = TourApplicationStatus.WITHDRAWN;
            application.setStatus(targetStatus);
            application.setCancellationReason(reason);
        } else if (oldStatus == TourApplicationStatus.ACCEPTED) {
            if (tour.getStartDate().isBefore(LocalDate.now())) {
                throw new InvalidApplicationStateException("Cannot cancel participation in a tour that has already started");
            }
            targetStatus = TourApplicationStatus.CANCELLED_BY_TRAVELER;
            application.setStatus(targetStatus);
            application.setCancellationReason(reason);

            // Free up the reserved place atomically
            tour.setAvailablePlaces(Math.min(tour.getCapacity(), tour.getAvailablePlaces() + 1));
            tourRepository.save(tour);
        } else {
            throw new InvalidApplicationStateException("Only PENDING or ACCEPTED applications can be cancelled/withdrawn");
        }

        TourApplication saved = tourApplicationRepository.save(application);

        ApplicationStatusHistory history = ApplicationStatusHistory.builder()
                .application(saved)
                .oldStatus(oldStatus)
                .newStatus(targetStatus)
                .changedBy(applicant)
                .reason(reason)
                .build();
        applicationStatusHistoryRepository.save(history);

        log.info("Application {} cancelled/withdrawn by traveler {}: new status={}", applicationId, currentUserId, targetStatus);
        if (oldStatus == TourApplicationStatus.ACCEPTED) {
            try {
                chatService.removeTourParticipantFromChat(tourId, currentUserId);
            } catch (Exception e) {
                log.warn("Failed to remove participant from tour chat", e);
            }
        }

        eventPublisher.publishEvent(new com.tourhive.backend.events.NotificationEvent(
                tour.getOrganizer().getId(),
                com.tourhive.backend.entities.notifications.NotificationType.APPLICATION_CANCELLED,
                "Application Cancelled",
                "A participant cancelled their application for tour '" + tour.getTitle() + "'",
                "APPLICATION",
                saved.getId()
        ));

        UserProfile profile = userProfileRepository.findByUserId(currentUserId).orElse(null);
        List<ApplicationStatusHistory> historyList = applicationStatusHistoryRepository.findAllByApplicationIdOrderByCreatedAtAsc(applicationId);
        return tourApplicationMapper.toResponseDto(saved, profile, historyList);
    }

    @Override
    public TourApplicationResponseDto getApplicationById(UUID tourId, UUID applicationId, UUID currentUserId) {
        TourApplication application = tourApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException("Application not found: " + applicationId));

        if (!application.getTour().getId().equals(tourId)) {
            throw new ApplicationNotFoundException("Application does not belong to tour: " + tourId);
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUserId));

        boolean isApplicant = application.getApplicant().getId().equals(currentUserId);
        boolean isOrganizer = application.getTour().getOrganizer().getId().equals(currentUserId);
        boolean isModerator = currentUser.getRole() == Role.MODERATOR || currentUser.getRole() == Role.ADMIN;

        if (!isApplicant && !isOrganizer && !isModerator) {
            throw new ApplicationAccessDeniedException("You do not have permission to view this application");
        }

        UserProfile profile = userProfileRepository.findByUserId(application.getApplicant().getId()).orElse(null);
        List<ApplicationStatusHistory> historyList = applicationStatusHistoryRepository.findAllByApplicationIdOrderByCreatedAtAsc(applicationId);
        return tourApplicationMapper.toResponseDto(application, profile, historyList);
    }

    @Override
    public Page<TourApplicationShortResponseDto> getTourApplications(UUID tourId, UUID currentUserId, TourApplicationStatus statusFilter, Pageable pageable) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new TourNotFoundException("Tour not found: " + tourId));

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUserId));

        boolean isOrganizer = tour.getOrganizer().getId().equals(currentUserId);
        boolean isModerator = currentUser.getRole() == Role.MODERATOR || currentUser.getRole() == Role.ADMIN;

        if (!isOrganizer && !isModerator) {
            throw new ApplicationAccessDeniedException("Only tour organizer or moderator can view tour applications");
        }

        Page<TourApplication> page;
        if (statusFilter != null) {
            page = tourApplicationRepository.findAllByTourIdAndStatus(tourId, statusFilter, pageable);
        } else {
            page = tourApplicationRepository.findAllByTourId(tourId, pageable);
        }

        return page.map(app -> {
            UserProfile profile = userProfileRepository.findByUserId(app.getApplicant().getId()).orElse(null);
            return tourApplicationMapper.toShortResponseDto(app, profile);
        });
    }

    @Override
    public Page<TourApplicationShortResponseDto> getMyApplications(UUID applicantId, TourApplicationStatus statusFilter, Pageable pageable) {
        Page<TourApplication> page;
        if (statusFilter != null) {
            page = tourApplicationRepository.findAllByApplicantIdAndStatus(applicantId, statusFilter, pageable);
        } else {
            page = tourApplicationRepository.findAllByApplicantId(applicantId, pageable);
        }

        return page.map(app -> {
            UserProfile profile = userProfileRepository.findByUserId(applicantId).orElse(null);
            return tourApplicationMapper.toShortResponseDto(app, profile);
        });
    }
}
