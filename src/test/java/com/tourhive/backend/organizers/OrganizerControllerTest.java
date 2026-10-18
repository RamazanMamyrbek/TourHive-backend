package com.tourhive.backend.organizers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourhive.backend.configurations.security.JwtTokenProvider;
import com.tourhive.backend.dto.organizers.request.BusinessDocumentRequestDto;
import com.tourhive.backend.dto.organizers.request.RegisterBusinessProfileRequestDto;
import com.tourhive.backend.entities.organizers.BusinessVerificationStatus;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrganizerControllerTest {

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

    private User guideUser;
    private String token;

    @BeforeEach
    void setUp() {
        databaseCleanup.clean();

        guideUser = userRepository.save(User.builder()
                .email("guide@tourhive.com")
                .passwordHash("hash")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .isOrganizer(false)
                .build());

        token = jwtTokenProvider.generateAccessToken(guideUser);
    }

    @Test
    @DisplayName("POST /api/v1/organizers/me/activate should activate organizer role and return profile")
    void shouldActivateOrganizer() throws Exception {
        mockMvc.perform(post("/api/v1/organizers/me/activate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isOrganizer").value(true))
                .andExpect(jsonPath("$.data.userId").value(guideUser.getId().toString()));

        User updated = userRepository.findById(guideUser.getId()).orElseThrow();
        assertThat(updated.isOrganizer()).isTrue();
        assertThat(organizerProfileRepository.findByUserId(guideUser.getId())).isPresent();
    }

    @Test
    @DisplayName("Full business profile application flow: submit -> view -> duplicate conflict check")
    void shouldHandleBusinessProfileLifecycle() throws Exception {
        RegisterBusinessProfileRequestDto request = new RegisterBusinessProfileRequestDto(
                "Steppe Nomads LLP",
                "BIN9876543210",
                "Kazakhstan",
                "info@steppenomads.kz",
                "+77029998877",
                "Eco-tourism and horse trekking across Almaty region",
                List.of(
                        new BusinessDocumentRequestDto("Certificate of Incorporation", "https://minio.local/docs/incorp.pdf"),
                        new BusinessDocumentRequestDto("Tour Operator License", "https://minio.local/docs/license.pdf")
                )
        );

        // 1. Submit application
        mockMvc.perform(post("/api/v1/organizers/me/business")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.legalName").value("Steppe Nomads LLP"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.documents", hasSize(2)));

        // 2. View current business profile
        mockMvc.perform(get("/api/v1/organizers/me/business")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.legalName").value("Steppe Nomads LLP"))
                .andExpect(jsonPath("$.data.registrationNumber").value("BIN9876543210"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        // 3. Attempt submitting duplicate while PENDING -> 409 Conflict
        mockMvc.perform(post("/api/v1/organizers/me/business")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BUSINESS_PROFILE_ALREADY_EXISTS"));

        // 4. View public organizer profile
        mockMvc.perform(get("/api/v1/organizers/" + guideUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(guideUser.getId().toString()))
                .andExpect(jsonPath("$.data.isOrganizer").value(true))
                .andExpect(jsonPath("$.data.isBusinessVerified").value(false));
    }
}
