package com.tourhive.backend.configurations.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourhive.backend.dto.errors.ApiErrorResponseDto;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserStatus;
import com.tourhive.backend.repositories.users.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                UUID userId = tokenProvider.getUserIdFromToken(jwt);
                User user = userRepository.findById(userId).orElse(null);

                if (user == null || user.getStatus() == UserStatus.DELETED) {
                    sendForbiddenResponse(response, request.getRequestURI(), "ACCOUNT_DELETED", "This user account has been deleted");
                    return;
                }

                if (user.getStatus() == UserStatus.BLOCKED) {
                    sendForbiddenResponse(response, request.getRequestURI(), "ACCOUNT_BLOCKED",
                            "This user account is blocked: " + (user.getBlockReason() != null ? user.getBlockReason() : "Please contact support"));
                    return;
                }

                UserPrincipal userPrincipal = new UserPrincipal(
                        user.getId(),
                        user.getEmail(),
                        "",
                        user.getRole(),
                        user.getStatus()
                );

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    private void sendForbiddenResponse(HttpServletResponse response, String uri, String code, String message) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiErrorResponseDto error = ApiErrorResponseDto.of(
                HttpStatus.FORBIDDEN.value(),
                code,
                message,
                uri
        );
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
