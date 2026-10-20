package com.tourhive.backend.configurations.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourhive.backend.dto.errors.ApiErrorResponseDto;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();

    private static final int AUTH_LIMIT_PER_MINUTE = 100;
    private static final int REPORT_LIMIT_PER_MINUTE = 50;

    public RateLimitingFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        if (uri.startsWith("/api/v1/auth/") || (uri.equals("/api/v1/reports") && "POST".equalsIgnoreCase(method))) {
            String clientIp = getClientIp(request);
            int maxRequests = uri.startsWith("/api/v1/auth/") ? AUTH_LIMIT_PER_MINUTE : REPORT_LIMIT_PER_MINUTE;
            String key = clientIp + ":" + (uri.startsWith("/api/v1/auth/") ? "auth" : "report");

            long currentMinute = System.currentTimeMillis() / 60000;
            RequestCounter counter = requestCounts.compute(key, (k, existing) -> {
                if (existing == null || existing.minute() != currentMinute) {
                    return new RequestCounter(currentMinute, new AtomicInteger(1));
                }
                existing.count().incrementAndGet();
                return existing;
            });

            if (counter.count().get() > maxRequests) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                ApiErrorResponseDto error = ApiErrorResponseDto.of(
                        HttpStatus.TOO_MANY_REQUESTS.value(),
                        "RATE_LIMIT_EXCEEDED",
                        "Too many requests. Please try again later.",
                        uri
                );
                response.getWriter().write(objectMapper.writeValueAsString(error));
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private record RequestCounter(long minute, AtomicInteger count) {}
}
