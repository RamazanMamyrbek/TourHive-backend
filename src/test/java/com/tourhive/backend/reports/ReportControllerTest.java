package com.tourhive.backend.reports;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourhive.backend.dto.reports.request.CreateReportRequestDto;
import com.tourhive.backend.dto.reports.request.ResolveReportRequestDto;
import com.tourhive.backend.entities.reports.ReportActionTaken;
import com.tourhive.backend.entities.reports.ReportReason;
import com.tourhive.backend.entities.reports.ReportStatus;
import com.tourhive.backend.entities.reports.ReportTargetType;
import com.tourhive.backend.entities.users.Role;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserStatus;
import com.tourhive.backend.repositories.reports.ReportRepository;
import com.tourhive.backend.repositories.users.UserRepository;
import com.tourhive.backend.configurations.security.JwtTokenProvider;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private DatabaseCleanup databaseCleanup;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User reporter;
    private User reportedUser;
    private User moderator;
    private String reporterToken;
    private String moderatorToken;

    @BeforeEach
    void setUp() {
        databaseCleanup.clean();

        reporter = userRepository.save(User.builder()
                .email("reporter@tourhive.com")
                .passwordHash("hash")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build());

        reportedUser = userRepository.save(User.builder()
                .email("baduser@tourhive.com")
                .passwordHash("hash")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build());

        moderator = userRepository.save(User.builder()
                .email("mod@tourhive.com")
                .passwordHash("hash")
                .role(Role.MODERATOR)
                .status(UserStatus.ACTIVE)
                .build());

        reporterToken = jwtTokenProvider.generateAccessToken(reporter);
        moderatorToken = jwtTokenProvider.generateAccessToken(moderator);
    }

    @Test
    @DisplayName("POST /api/v1/reports - success creates report")
    void createReport_Success() throws Exception {
        CreateReportRequestDto request = new CreateReportRequestDto(
                ReportTargetType.USER,
                reportedUser.getId(),
                ReportReason.SPAM,
                "Sending unsolicited promotional messages."
        );

        mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + reporterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.targetType").value("USER"))
                .andExpect(jsonPath("$.data.targetId").value(reportedUser.getId().toString()))
                .andExpect(jsonPath("$.data.reason").value("SPAM"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /api/v1/reports - self report throws 400")
    void createReport_SelfReport_BadRequest() throws Exception {
        CreateReportRequestDto request = new CreateReportRequestDto(
                ReportTargetType.USER,
                reporter.getId(),
                ReportReason.OTHER,
                "Reporting myself"
        );

        mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + reporterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/reports - duplicate active report throws 409")
    void createReport_Duplicate_Conflict() throws Exception {
        CreateReportRequestDto request = new CreateReportRequestDto(
                ReportTargetType.USER,
                reportedUser.getId(),
                ReportReason.FRAUD,
                "Scam profile"
        );

        mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + reporterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + reporterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET /api/v1/reports/my - returns current user's submitted reports")
    void getMyReports_Success() throws Exception {
        CreateReportRequestDto request = new CreateReportRequestDto(
                ReportTargetType.USER,
                reportedUser.getId(),
                ReportReason.HARASSMENT,
                "Harassing in chat"
        );

        mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + reporterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/reports/my")
                        .header("Authorization", "Bearer " + reporterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].reason").value("HARASSMENT"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/reports - non-admin forbidden 403")
    void getAdminReports_ForbiddenForRegularUser() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reports")
                        .header("Authorization", "Bearer " + reporterToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/admin/reports/{id}/resolve - moderator resolves report")
    void resolveReport_Success() throws Exception {
        CreateReportRequestDto createDto = new CreateReportRequestDto(
                ReportTargetType.USER,
                reportedUser.getId(),
                ReportReason.SPAM,
                "Spam account"
        );

        String responseBody = mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + reporterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String reportId = objectMapper.readTree(responseBody).get("data").get("id").asText();

        ResolveReportRequestDto resolveDto = new ResolveReportRequestDto(
                ReportStatus.RESOLVED,
                ReportActionTaken.USER_BLOCKED,
                "Account suspended for violation"
        );

        mockMvc.perform(post("/api/v1/admin/reports/" + reportId + "/resolve")
                        .header("Authorization", "Bearer " + moderatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resolveDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(reportId))
                .andExpect(jsonPath("$.data.status").value("RESOLVED"))
                .andExpect(jsonPath("$.data.actionTaken").value("USER_BLOCKED"))
                .andExpect(jsonPath("$.data.resolutionNotes").value("Account suspended for violation"));
    }
}
