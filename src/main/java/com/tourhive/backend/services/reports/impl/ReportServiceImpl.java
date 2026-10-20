package com.tourhive.backend.services.reports.impl;

import com.tourhive.backend.dto.reports.request.CreateReportRequestDto;
import com.tourhive.backend.dto.reports.request.ResolveReportRequestDto;
import com.tourhive.backend.dto.reports.response.ReportDetailResponseDto;
import com.tourhive.backend.dto.reports.response.ReportResponseDto;
import com.tourhive.backend.entities.audit.AuditAction;
import com.tourhive.backend.entities.chats.Message;
import com.tourhive.backend.entities.reports.Report;
import com.tourhive.backend.entities.reports.ReportActionTaken;
import com.tourhive.backend.entities.reports.ReportStatus;
import com.tourhive.backend.entities.reports.ReportTargetType;
import com.tourhive.backend.entities.reviews.Review;
import com.tourhive.backend.entities.tours.Tour;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserStatus;
import com.tourhive.backend.exceptions.CannotReportOwnContentException;
import com.tourhive.backend.exceptions.DuplicateReportException;
import com.tourhive.backend.exceptions.ReportNotFoundException;
import com.tourhive.backend.exceptions.TargetNotFoundException;
import com.tourhive.backend.mappers.reports.ReportMapper;
import com.tourhive.backend.repositories.auth.RefreshTokenRepository;
import com.tourhive.backend.repositories.chats.MessageRepository;
import com.tourhive.backend.repositories.reports.ReportRepository;
import com.tourhive.backend.repositories.reviews.ReviewRepository;
import com.tourhive.backend.repositories.tours.TourRepository;
import com.tourhive.backend.repositories.users.UserRepository;
import com.tourhive.backend.services.audit.AuditService;
import com.tourhive.backend.services.reports.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final ReportMapper reportMapper;
    private final UserRepository userRepository;
    private final TourRepository tourRepository;
    private final MessageRepository messageRepository;
    private final ReviewRepository reviewRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditService auditService;

    @Override
    @Transactional
    public ReportResponseDto createReport(UUID reporterId, CreateReportRequestDto request) {
        validateTargetAndOwnership(reporterId, request.targetType(), request.targetId());

        boolean activeReportExists = reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatusIn(
                reporterId,
                request.targetType(),
                request.targetId(),
                List.of(ReportStatus.PENDING, ReportStatus.INVESTIGATING)
        );

        if (activeReportExists) {
            throw new DuplicateReportException("You already have an active pending report for this " + request.targetType().name().toLowerCase());
        }

        Report report = Report.builder()
                .reporterId(reporterId)
                .targetType(request.targetType())
                .targetId(request.targetId())
                .reason(request.reason())
                .description(request.description())
                .status(ReportStatus.PENDING)
                .build();

        Report saved = reportRepository.save(report);
        log.info("Report created: id={} reporterId={} targetType={} targetId={}",
                saved.getId(), reporterId, saved.getTargetType(), saved.getTargetId());

        return reportMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReportResponseDto> getMyReports(UUID reporterId, Pageable pageable) {
        return reportRepository.findAllByReporterId(reporterId, pageable).map(reportMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReportResponseDto> getReports(ReportStatus status, ReportTargetType targetType, Pageable pageable) {
        Page<Report> page;
        if (status != null && targetType != null) {
            page = reportRepository.findAllByStatusAndTargetType(status, targetType, pageable);
        } else if (status != null) {
            page = reportRepository.findAllByStatus(status, pageable);
        } else if (targetType != null) {
            page = reportRepository.findAllByTargetType(targetType, pageable);
        } else {
            page = reportRepository.findAll(pageable);
        }
        return page.map(reportMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportDetailResponseDto getReportDetails(UUID reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException("Report not found: " + reportId));

        String reporterEmail = userRepository.findById(report.getReporterId())
                .map(User::getEmail)
                .orElse("unknown");

        String targetSummary = resolveTargetSummary(report.getTargetType(), report.getTargetId());

        return reportMapper.toDetailDto(report, reporterEmail, targetSummary);
    }

    @Override
    @Transactional
    public ReportResponseDto resolveReport(UUID reportId, UUID resolverId, String resolverEmail, ResolveReportRequestDto request, String ipAddress) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException("Report not found: " + reportId));

        report.setStatus(request.status());
        report.setActionTaken(request.actionTaken());
        report.setResolutionNotes(request.resolutionNotes());
        report.setResolvedBy(resolverId);
        report.setResolvedAt(Instant.now());

        if (request.actionTaken() == ReportActionTaken.USER_BLOCKED && report.getTargetType() == ReportTargetType.USER) {
            userRepository.findById(report.getTargetId()).ifPresent(userToBlock -> {
                userToBlock.setStatus(UserStatus.BLOCKED);
                userToBlock.setBlockReason("Blocked due to report #" + report.getId() + ": " + request.resolutionNotes());
                userRepository.save(userToBlock);
                refreshTokenRepository.revokeAllByUserId(userToBlock.getId());
                log.info("User {} blocked as a result of report #{}", userToBlock.getId(), report.getId());
            });
        }

        AuditAction auditAction = request.status() == ReportStatus.RESOLVED ? AuditAction.REPORT_RESOLVED : AuditAction.REPORT_DISMISSED;
        auditService.logAction(
                resolverId,
                resolverEmail,
                auditAction,
                "REPORT",
                report.getId(),
                "Status: " + request.status() + ", Action: " + request.actionTaken() + ", Notes: " + request.resolutionNotes(),
                ipAddress
        );

        Report saved = reportRepository.save(report);
        log.info("Report #{} resolved by {} with status {} and action {}", reportId, resolverEmail, request.status(), request.actionTaken());
        return reportMapper.toDto(saved);
    }

    private void validateTargetAndOwnership(UUID reporterId, ReportTargetType targetType, UUID targetId) {
        switch (targetType) {
            case USER -> {
                if (reporterId.equals(targetId)) {
                    throw new CannotReportOwnContentException("You cannot report your own profile");
                }
                if (!userRepository.existsById(targetId)) {
                    throw new TargetNotFoundException("User not found: " + targetId);
                }
            }
            case TOUR -> {
                Tour tour = tourRepository.findById(targetId)
                        .orElseThrow(() -> new TargetNotFoundException("Tour not found: " + targetId));
                if (tour.getOrganizer().getId().equals(reporterId)) {
                    throw new CannotReportOwnContentException("You cannot report your own tour");
                }
            }
            case MESSAGE -> {
                Message message = messageRepository.findById(targetId)
                        .orElseThrow(() -> new TargetNotFoundException("Message not found: " + targetId));
                if (message.getSender().getId().equals(reporterId)) {
                    throw new CannotReportOwnContentException("You cannot report your own message");
                }
            }
            case REVIEW -> {
                Review review = reviewRepository.findById(targetId)
                        .orElseThrow(() -> new TargetNotFoundException("Review not found: " + targetId));
                if (review.getAuthor().getId().equals(reporterId)) {
                    throw new CannotReportOwnContentException("You cannot report your own review");
                }
            }
        }
    }

    private String resolveTargetSummary(ReportTargetType targetType, UUID targetId) {
        return switch (targetType) {
            case USER -> userRepository.findById(targetId).map(u -> "User: " + u.getEmail()).orElse("User [not found]");
            case TOUR -> tourRepository.findById(targetId).map(t -> "Tour: " + t.getTitle()).orElse("Tour [not found]");
            case MESSAGE -> messageRepository.findById(targetId).map(m -> "Message: " + (m.getContent() != null ? m.getContent() : "[attachment]")).orElse("Message [not found]");
            case REVIEW -> reviewRepository.findById(targetId).map(r -> "Review: Rating " + r.getRating() + " - " + r.getComment()).orElse("Review [not found]");
        };
    }
}
