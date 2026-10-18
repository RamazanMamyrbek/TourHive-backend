package com.tourhive.backend.configurations;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocumentationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /v3/api-docs should return OpenAPI spec without authentication")
    void apiDocsEndpointShouldReturnSpec() throws Exception {
        mockMvc.perform(get("/v3/api-docs")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.info.title").value("TourHive API"))
                .andExpect(jsonPath("$.info.version").value("1.0.0"));
    }

    @Test
    @DisplayName("GET /swagger-ui/index.html should be accessible without authentication")
    void swaggerUiShouldBeAccessible() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().is3xxRedirection());
    }
}
