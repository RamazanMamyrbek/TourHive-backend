package com.tourhive.backend.services.admin.impl;

import com.tourhive.backend.dto.admin.request.ReviewTourRequestDto;
import com.tourhive.backend.dto.tours.response.TourResponseDto;
import com.tourhive.backend.entities.tours.Tour;
import com.tourhive.backend.entities.tours.TourStatus;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserProfile;
import com.tourhive.backend.exceptions.InvalidTourDataException;
import com.tourhive.backend.exceptions.TourNotFoundException;
import com.tourhive.backend.mappers.tours.TourMapper;
import com.tourhive.backend.repositories.tours.TourRepository;
import com.tourhive.backend.repositories.users.UserProfileRepository;
import com.tourhive.backend.services.admin.AdminTourService;
import com.tourhive.backend.services.users.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import com.tourhive.backend.entities.audit.AuditAction;
import com.tourhive.backend.services.audit.AuditService;

@Slf4j
@Service
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
public class AdminTourServiceImpl implements AdminTourService {

    private final TourRepository tourRepository;
    private final UserService userService;
    private final UserProfileRepository userProfileRepository;
    private final TourMapper tourMapper;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public Page<TourResponseDto> getPendingTours(Pageable pageable) {
        return tourRepository.findAll(
                (root, query, cb) -> cb.equal(root.get("status"), TourStatus.PENDING_REVIEW),
                pageable
        ).map(tour -> {
            UserProfile profile = userProfileRepository.findByUserId(tour.getOrganizer().getId()).orElse(null);
            return tourMapper.toResponseDto(tour, profile);
        });
    }

    @Override
    @Transactional
    public TourResponseDto reviewTour(UUID reviewerId, UUID tourId, ReviewTourRequestDto request) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new TourNotFoundException("Tour not found: " + tourId));

        if (tour.getStatus() != TourStatus.PENDING_REVIEW) {
            throw new InvalidTourDataException("Only tours in PENDING_REVIEW status can be reviewed");
        }

        if (request.status() != TourStatus.PUBLISHED && request.status() != TourStatus.REJECTED) {
            throw new InvalidTourDataException("Review decision must be either PUBLISHED or REJECTED");
        }

        if (request.status() == TourStatus.REJECTED && (request.rejectionReason() == null || request.rejectionReason().isBlank())) {
            throw new InvalidTourDataException("Rejection reason is required when rejecting a tour");
        }

        User reviewer = userService.getById(reviewerId);
        tour.setStatus(request.status());
        tour.setReviewedBy(reviewer);
        tour.setReviewedAt(Instant.now());

        if (request.status() == TourStatus.PUBLISHED) {
            tour.setPublishedAt(Instant.now());
            tour.setRejectionReason(null);
        } else {
            tour.setRejectionReason(request.rejectionReason());
        }

        Tour saved = tourRepository.save(tour);
        log.info("Moderator {} reviewed tour {}: status={}", reviewerId, tourId, request.status());

        AuditAction auditAction = request.status() == TourStatus.PUBLISHED ? AuditAction.TOUR_APPROVED : AuditAction.TOUR_REJECTED;
        auditService.logAction(
                reviewerId,
                reviewer.getEmail(),
                auditAction,
                "TOUR",
                tour.getId(),
                "Status: " + request.status() + (request.rejectionReason() != null ? ", Reason: " + request.rejectionReason() : ""),
                null
        );

        if (request.status() == TourStatus.PUBLISHED) {
            eventPublisher.publishEvent(new com.tourhive.backend.events.NotificationEvent(
                    tour.getOrganizer().getId(),
                    com.tourhive.backend.entities.notifications.NotificationType.TOUR_APPROVED,
                    "Tour Published",
                    "Your tour '" + tour.getTitle() + "' has been approved and published!",
                    "TOUR",
                    tour.getId()
            ));
        } else if (request.status() == TourStatus.REJECTED) {
            eventPublisher.publishEvent(new com.tourhive.backend.events.NotificationEvent(
                    tour.getOrganizer().getId(),
                    com.tourhive.backend.entities.notifications.NotificationType.TOUR_REJECTED,
                    "Tour Rejected",
                    "Your tour '" + tour.getTitle() + "' was rejected. Reason: " + request.rejectionReason(),
                    "TOUR",
                    tour.getId()
            ));
        }

        UserProfile profile = userProfileRepository.findByUserId(tour.getOrganizer().getId()).orElse(null);
        return tourMapper.toResponseDto(saved, profile);
    }
}
