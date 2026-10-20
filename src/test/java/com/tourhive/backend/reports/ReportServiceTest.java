package com.tourhive.backend.reports;

import com.tourhive.backend.dto.reports.request.CreateReportRequestDto;
import com.tourhive.backend.dto.reports.request.ResolveReportRequestDto;
import com.tourhive.backend.dto.reports.response.ReportDetailResponseDto;
import com.tourhive.backend.dto.reports.response.ReportResponseDto;
import com.tourhive.backend.entities.reports.Report;
import com.tourhive.backend.entities.reports.ReportActionTaken;
import com.tourhive.backend.entities.reports.ReportReason;
import com.tourhive.backend.entities.reports.ReportStatus;
import com.tourhive.backend.entities.reports.ReportTargetType;
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
import com.tourhive.backend.services.reports.impl.ReportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TourRepository tourRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private AuditService auditService;

    @Spy
    private ReportMapper reportMapper = Mappers.getMapper(ReportMapper.class);

    @InjectMocks
    private ReportServiceImpl reportService;

    private UUID reporterId;
    private UUID targetUserId;
    private UUID tourId;
    private User targetUser;
    private Tour targetTour;

    @BeforeEach
    void setUp() {
        reporterId = UUID.randomUUID();
        targetUserId = UUID.randomUUID();
        tourId = UUID.randomUUID();

        targetUser = User.builder()
                .email("target@example.com")
                .status(UserStatus.ACTIVE)
                .build();
        targetUser.setId(targetUserId);

        targetTour = Tour.builder()
                .organizer(targetUser)
                .title("Mountain Hike")
                .build();
        targetTour.setId(tourId);
    }

    @Test
    @DisplayName("Successfully create report on user")
    void createReportOnUserSuccess() {
        when(userRepository.existsById(targetUserId)).thenReturn(true);
        when(reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatusIn(any(), any(), any(), any())).thenReturn(false);
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> {
            Report r = invocation.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        CreateReportRequestDto request = new CreateReportRequestDto(
                ReportTargetType.USER,
                targetUserId,
                ReportReason.SPAM,
                "Spamming commercial links"
        );

        ReportResponseDto response = reportService.createReport(reporterId, request);

        assertThat(response).isNotNull();
        assertThat(response.targetType()).isEqualTo(ReportTargetType.USER);
        assertThat(response.reason()).isEqualTo(ReportReason.SPAM);
        assertThat(response.status()).isEqualTo(ReportStatus.PENDING);
        verify(reportRepository).save(any(Report.class));
    }

    @Test
    @DisplayName("Should throw CannotReportOwnContentException when reporting self")
    void createReportOnSelfThrowsException() {
        CreateReportRequestDto request = new CreateReportRequestDto(
                ReportTargetType.USER,
                reporterId,
                ReportReason.SPAM,
                "Self report"
        );

        assertThatThrownBy(() -> reportService.createReport(reporterId, request))
                .isInstanceOf(CannotReportOwnContentException.class);
    }

    @Test
    @DisplayName("Should throw CannotReportOwnContentException when reporting own tour")
    void createReportOnOwnTourThrowsException() {
        targetTour.setOrganizer(User.builder().build());
        targetTour.getOrganizer().setId(reporterId);

        when(tourRepository.findById(tourId)).thenReturn(Optional.of(targetTour));

        CreateReportRequestDto request = new CreateReportRequestDto(
                ReportTargetType.TOUR,
                tourId,
                ReportReason.FRAUD,
                "Reporting my own tour"
        );

        assertThatThrownBy(() -> reportService.createReport(reporterId, request))
                .isInstanceOf(CannotReportOwnContentException.class);
    }

    @Test
    @DisplayName("Should throw TargetNotFoundException when target does not exist")
    void createReportNonExistentTargetThrowsException() {
        when(userRepository.existsById(targetUserId)).thenReturn(false);

        CreateReportRequestDto request = new CreateReportRequestDto(
                ReportTargetType.USER,
                targetUserId,
                ReportReason.HARASSMENT,
                "User not found"
        );

        assertThatThrownBy(() -> reportService.createReport(reporterId, request))
                .isInstanceOf(TargetNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw DuplicateReportException when active report exists")
    void createReportDuplicateActiveThrowsException() {
        when(userRepository.existsById(targetUserId)).thenReturn(true);
        when(reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatusIn(any(), any(), any(), any())).thenReturn(true);

        CreateReportRequestDto request = new CreateReportRequestDto(
                ReportTargetType.USER,
                targetUserId,
                ReportReason.SPAM,
                "Duplicate report"
        );

        assertThatThrownBy(() -> reportService.createReport(reporterId, request))
                .isInstanceOf(DuplicateReportException.class);
    }

    @Test
    @DisplayName("Resolve report with USER_BLOCKED action should block reported user")
    void resolveReportWithUserBlocked() {
        UUID reportId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        Report report = Report.builder()
                .reporterId(reporterId)
                .targetType(ReportTargetType.USER)
                .targetId(targetUserId)
                .reason(ReportReason.FRAUD)
                .description("Confirmed fraudster")
                .status(ReportStatus.PENDING)
                .build();
        report.setId(reportId);

        when(reportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(reportRepository.save(any(Report.class))).thenAnswer(inv -> inv.getArgument(0));

        ResolveReportRequestDto request = new ResolveReportRequestDto(
                ReportStatus.RESOLVED,
                ReportActionTaken.USER_BLOCKED,
                "Banned for fraud"
        );

        ReportResponseDto response = reportService.resolveReport(reportId, adminId, "admin@tourhive.com", request, "127.0.0.1");

        assertThat(response.status()).isEqualTo(ReportStatus.RESOLVED);
        assertThat(response.actionTaken()).isEqualTo(ReportActionTaken.USER_BLOCKED);
        assertThat(targetUser.getStatus()).isEqualTo(UserStatus.BLOCKED);
        assertThat(targetUser.getBlockReason()).contains("Banned for fraud");
        verify(refreshTokenRepository).revokeAllByUserId(targetUserId);
        verify(auditService).logAction(eq(adminId), eq("admin@tourhive.com"), any(), eq("REPORT"), eq(reportId), any(), any());
    }

    @Test
    @DisplayName("Get report details returns populated target summary")
    void getReportDetailsSuccess() {
        UUID reportId = UUID.randomUUID();
        Report report = Report.builder()
                .reporterId(reporterId)
                .targetType(ReportTargetType.TOUR)
                .targetId(tourId)
                .reason(ReportReason.INAPPROPRIATE_CONTENT)
                .description("Offensive tour description")
                .status(ReportStatus.PENDING)
                .build();
        report.setId(reportId);

        when(reportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(userRepository.findById(reporterId)).thenReturn(Optional.of(User.builder().email("reporter@tourhive.com").build()));
        when(tourRepository.findById(tourId)).thenReturn(Optional.of(targetTour));

        ReportDetailResponseDto details = reportService.getReportDetails(reportId);

        assertThat(details).isNotNull();
        assertThat(details.reporterEmail()).isEqualTo("reporter@tourhive.com");
        assertThat(details.targetSummary()).contains("Mountain Hike");
    }
}
