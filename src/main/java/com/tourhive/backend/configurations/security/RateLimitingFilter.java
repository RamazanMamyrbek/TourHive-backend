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
    private static final int OTP_LIMIT_PER_MINUTE = 10;
    private static final int REPORT_LIMIT_PER_MINUTE = 50;
    private static final int MAX_CACHE_SIZE = 5000;

    public RateLimitingFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        boolean isOtpEndpoint = uri.equals("/api/v1/auth/verify-email")
                || uri.equals("/api/v1/auth/reset-password")
                || uri.equals("/api/v1/auth/forgot-password")
                || uri.equals("/api/v1/auth/resend-code");

        boolean isAuthEndpoint = uri.startsWith("/api/v1/auth/");
        boolean isReportEndpoint = uri.equals("/api/v1/reports") && "POST".equalsIgnoreCase(method);

        if (isAuthEndpoint || isReportEndpoint) {
            String clientIp = getClientIp(request);
            int maxRequests = isOtpEndpoint ? OTP_LIMIT_PER_MINUTE : (isAuthEndpoint ? AUTH_LIMIT_PER_MINUTE : REPORT_LIMIT_PER_MINUTE);
            String category = isOtpEndpoint ? "otp" : (isAuthEndpoint ? "auth" : "report");
            String key = clientIp + ":" + category;

            long currentMinute = System.currentTimeMillis() / 60000;

            if (requestCounts.size() > MAX_CACHE_SIZE) {
                requestCounts.entrySet().removeIf(entry -> entry.getValue().minute() < currentMinute);
            }

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
        String remoteAddr = request.getRemoteAddr();
        if (isTrustedProxy(remoteAddr)) {
            String xf = request.getHeader("X-Forwarded-For");
            if (xf != null && !xf.isBlank()) {
                return xf.split(",")[0].trim();
            }
        }
        return remoteAddr != null ? remoteAddr : "unknown";
    }

    private boolean isTrustedProxy(String ip) {
        if (ip == null) {
            return false;
        }
        return "127.0.0.1".equals(ip)
                || "0:0:0:0:0:0:0:1".equals(ip)
                || "::1".equals(ip)
                || ip.startsWith("10.")
                || ip.startsWith("192.168.")
                || isPrivate172(ip);
    }

    private boolean isPrivate172(String ip) {
        if (!ip.startsWith("172.")) {
            return false;
        }
        try {
            String[] parts = ip.split("\\.");
            if (parts.length >= 2) {
                int second = Integer.parseInt(parts[1]);
                return second >= 16 && second <= 31;
            }
        } catch (NumberFormatException ignored) {
        }
        return false;
    }

    private record RequestCounter(long minute, AtomicInteger count) {}
}
