package com.tourhive.backend.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourhive.backend.configurations.security.JwtTokenProvider;
import com.tourhive.backend.dto.admin.request.ReviewBusinessProfileRequestDto;
import com.tourhive.backend.entities.organizers.BusinessDocument;
import com.tourhive.backend.entities.organizers.BusinessProfile;
import com.tourhive.backend.entities.organizers.BusinessVerificationStatus;
import com.tourhive.backend.entities.organizers.OrganizerProfile;
import com.tourhive.backend.entities.users.Role;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserStatus;
import com.tourhive.backend.repositories.organizers.BusinessDocumentRepository;
import com.tourhive.backend.repositories.organizers.BusinessProfileRepository;
import com.tourhive.backend.repositories.organizers.OrganizerProfileRepository;
import com.tourhive.backend.repositories.users.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminVerificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizerProfileRepository organizerProfileRepository;

    @Autowired
    private BusinessProfileRepository businessProfileRepository;

    @Autowired
    private com.tourhive.backend.support.DatabaseCleanup databaseCleanup;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User regularUser;
    private User moderator;
    private User organizerUser;
    private BusinessProfile pendingProfile;
    private String userToken;
    private String moderatorToken;

    @BeforeEach
    void setUp() {
        databaseCleanup.clean();

        regularUser = userRepository.save(User.builder()
                .email("regular@tourhive.com")
                .passwordHash("hash")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build());

        moderator = userRepository.save(User.builder()
                .email("moderator@tourhive.com")
                .passwordHash("hash")
                .role(Role.MODERATOR)
                .status(UserStatus.ACTIVE)
                .build());

        organizerUser = userRepository.save(User.builder()
                .email("organizer@tourhive.com")
                .passwordHash("hash")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .isOrganizer(true)
                .isBusinessVerified(false)
                .build());

        organizerProfileRepository.save(OrganizerProfile.builder()
                .user(organizerUser)
                .experienceYears(3)
                .build());

        pendingProfile = BusinessProfile.builder()
                .user(organizerUser)
                .legalName("Silk Road Expeditions LLP")
                .registrationNumber("BIN5554443332")
                .country("Kazakhstan")
                .contactEmail("admin@silkroad.kz")
                .contactPhone("+77051234567")
                .description("Mountain guide operator")
                .status(BusinessVerificationStatus.PENDING)
                .build();
        pendingProfile.addDocument(BusinessDocument.builder()
                .documentName("State Registration Certificate")
                .documentUrl("https://minio.local/certs/cert1.pdf")
                .build());
        pendingProfile = businessProfileRepository.save(pendingProfile);

        userToken = jwtTokenProvider.generateAccessToken(regularUser);
        moderatorToken = jwtTokenProvider.generateAccessToken(moderator);
    }

    @Test
    @DisplayName("GET /api/v1/admin/business-verifications should return 403 Forbidden for regular USER")
    void shouldDenyAccessToRegularUser() throws Exception {
        mockMvc.perform(get("/api/v1/admin/business-verifications")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/business-verifications should list pending applications for MODERATOR")
    void shouldListPendingApplicationsForModerator() throws Exception {
        mockMvc.perform(get("/api/v1/admin/business-verifications")
                        .header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].legalName").value("Silk Road Expeditions LLP"))
                .andExpect(jsonPath("$.data.content[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/business-verifications/{id} should return full details with documents")
    void shouldReturnVerificationDetails() throws Exception {
        mockMvc.perform(get("/api/v1/admin/business-verifications/" + pendingProfile.getId())
                        .header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(pendingProfile.getId().toString()))
                .andExpect(jsonPath("$.data.documents", hasSize(1)))
                .andExpect(jsonPath("$.data.documents[0].documentName").value("State Registration Certificate"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/business-verifications/{id}/review with VERIFIED should approve and update user flag")
    void shouldApproveApplication() throws Exception {
        ReviewBusinessProfileRequestDto review = new ReviewBusinessProfileRequestDto(
                BusinessVerificationStatus.VERIFIED,
                null
        );

        mockMvc.perform(post("/api/v1/admin/business-verifications/" + pendingProfile.getId() + "/review")
                        .header("Authorization", "Bearer " + moderatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(review)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("VERIFIED"))
                .andExpect(jsonPath("$.data.reviewedByEmail").value("moderator@tourhive.com"));

        BusinessProfile updatedProfile = businessProfileRepository.findById(pendingProfile.getId()).orElseThrow();
        assertThat(updatedProfile.getStatus()).isEqualTo(BusinessVerificationStatus.VERIFIED);
        assertThat(updatedProfile.getReviewedBy().getId()).isEqualTo(moderator.getId());

        User updatedUser = userRepository.findById(organizerUser.getId()).orElseThrow();
        assertThat(updatedUser.isBusinessVerified()).isTrue();
    }

    @Test
    @DisplayName("POST /api/v1/admin/business-verifications/{id}/review with REJECTED should record reason")
    void shouldRejectApplication() throws Exception {
        ReviewBusinessProfileRequestDto review = new ReviewBusinessProfileRequestDto(
                BusinessVerificationStatus.REJECTED,
                "Submitted documents are expired or illegible"
        );

        mockMvc.perform(post("/api/v1/admin/business-verifications/" + pendingProfile.getId() + "/review")
                        .header("Authorization", "Bearer " + moderatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(review)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.rejectionReason").value("Submitted documents are expired or illegible"));

        BusinessProfile updatedProfile = businessProfileRepository.findById(pendingProfile.getId()).orElseThrow();
        assertThat(updatedProfile.getStatus()).isEqualTo(BusinessVerificationStatus.REJECTED);
        assertThat(updatedProfile.getRejectionReason()).isEqualTo("Submitted documents are expired or illegible");

        User updatedUser = userRepository.findById(organizerUser.getId()).orElseThrow();
        assertThat(updatedUser.isBusinessVerified()).isFalse();
    }
}
