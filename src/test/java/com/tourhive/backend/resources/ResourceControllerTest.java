package com.tourhive.backend.resources;

import com.tourhive.backend.configurations.security.JwtTokenProvider;
import com.tourhive.backend.dto.resources.response.FileDto;
import com.tourhive.backend.entities.users.Role;
import com.tourhive.backend.entities.users.UserStatus;
import com.tourhive.backend.entities.resources.Resource;
import com.tourhive.backend.entities.resources.ResourceType;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.exceptions.ResourceNotFoundException;
import com.tourhive.backend.repositories.users.UserRepository;
import com.tourhive.backend.services.resources.ResourceService;
import com.tourhive.backend.support.DatabaseCleanup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ResourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DatabaseCleanup databaseCleanup;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private ResourceService resourceService;

    private User testUser;
    private String token;

    @BeforeEach
    void setUp() {
        databaseCleanup.clean();

        testUser = userRepository.save(User.builder()
                .email("resourcetester@tourhive.com")
                .passwordHash("$2a$12$dummyhashforpasswordtesting123")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build());

        token = jwtTokenProvider.generateAccessToken(testUser);
    }

    @Test
    @DisplayName("GET /api/v1/resources/{id} should be public and stream file content")
    void shouldStreamResourceFilePublicly() throws Exception {
        UUID resourceId = UUID.randomUUID();
        byte[] fileBytes = "hello tourhive binary content".getBytes(StandardCharsets.UTF_8);
        FileDto fileDto = new FileDto(
                new InputStreamResource(new ByteArrayInputStream(fileBytes)),
                "my-photo.jpg",
                "image/jpeg"
        );

        Mockito.when(resourceService.getFileDtoById(resourceId)).thenReturn(fileDto);

        mockMvc.perform(get("/api/v1/resources/" + resourceId))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/jpeg"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"my-photo.jpg\""))
                .andExpect(header().string("File-Name", "my-photo.jpg"))
                .andExpect(content().bytes(fileBytes));
    }

    @Test
    @DisplayName("GET /api/v1/resources/{id} should return 404 when resource is not found")
    void shouldReturn404WhenResourceNotFound() throws Exception {
        UUID resourceId = UUID.randomUUID();
        Mockito.when(resourceService.getFileDtoById(resourceId))
                .thenThrow(new ResourceNotFoundException("Resource not found by id: " + resourceId));

        mockMvc.perform(get("/api/v1/resources/" + resourceId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("POST /api/v1/resources/upload should upload file and return ResourceResponseDto")
    void shouldUploadResource() throws Exception {
        UUID resourceId = UUID.randomUUID();
        Resource resource = new Resource(
                "test-doc.pdf",
                "documents/" + resourceId + ".pdf",
                "documents",
                1024L,
                ResourceType.PDF,
                "application/pdf"
        );
        resource.setId(resourceId);

        Mockito.when(resourceService.saveResource(any(), eq("documents"))).thenReturn(resource);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-doc.pdf",
                "application/pdf",
                "pdf data".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/resources/upload")
                        .file(file)
                        .param("folder", "documents")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(resourceId.toString()))
                .andExpect(jsonPath("$.data.name").value("test-doc.pdf"))
                .andExpect(jsonPath("$.data.url").value("/api/v1/resources/" + resourceId))
                .andExpect(jsonPath("$.data.contentType").value("PDF"))
                .andExpect(jsonPath("$.data.mimeType").value("application/pdf"));
    }

    @Test
    @DisplayName("POST /api/v1/resources/upload without auth should return 401")
    void shouldRejectUploadWithoutAuth() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-doc.pdf",
                "application/pdf",
                "pdf data".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/resources/upload")
                        .file(file))
                .andExpect(status().isUnauthorized());
    }
}
