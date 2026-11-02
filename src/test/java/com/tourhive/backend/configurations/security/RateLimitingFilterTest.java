package com.tourhive.backend.configurations.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RateLimitingFilterTest {

    private RateLimitingFilter filter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        filter = new RateLimitingFilter(objectMapper);
    }

    @Test
    @DisplayName("Should ignore X-Forwarded-For from untrusted remote address")
    void ignoreXffFromUntrustedRemote() throws ServletException, IOException {
        FilterChain filterChain = mock(FilterChain.class);

        // Untrusted external IP spoofing X-Forwarded-For
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/verify-email");
            request.setRemoteAddr("198.51.100.5");
            request.addHeader("X-Forwarded-For", "1.1.1." + i); // spoofing different IPs
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);
            assertThat(response.getStatus()).isEqualTo(200);
        }

        // 11th request from same untrusted IP should be rate limited despite different XFF
        MockHttpServletRequest blockedRequest = new MockHttpServletRequest("POST", "/api/v1/auth/verify-email");
        blockedRequest.setRemoteAddr("198.51.100.5");
        blockedRequest.addHeader("X-Forwarded-For", "1.1.1.99");
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();

        filter.doFilter(blockedRequest, blockedResponse, filterChain);
        assertThat(blockedResponse.getStatus()).isEqualTo(429);
    }

    @Test
    @DisplayName("Should trust X-Forwarded-For from loopback proxy")
    void trustXffFromLoopbackProxy() throws ServletException, IOException {
        FilterChain filterChain = mock(FilterChain.class);

        // Requests coming via 127.0.0.1 proxy with different client IPs should not interfere
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/verify-email");
            request.setRemoteAddr("127.0.0.1");
            request.addHeader("X-Forwarded-For", "203.0.113." + i);
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }
}
