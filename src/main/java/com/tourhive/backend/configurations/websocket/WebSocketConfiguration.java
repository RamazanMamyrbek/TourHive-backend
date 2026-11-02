package com.tourhive.backend.configurations.websocket;

import com.tourhive.backend.configurations.security.JwtTokenProvider;
import com.tourhive.backend.configurations.security.UserPrincipal;
import com.tourhive.backend.entities.users.Role;
import com.tourhive.backend.entities.users.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.repositories.chats.ChatMemberRepository;
import com.tourhive.backend.repositories.users.UserRepository;
import org.springframework.security.access.AccessDeniedException;

import java.security.Principal;
import java.util.UUID;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfiguration implements WebSocketMessageBrokerConfigurer {

    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final ChatMemberRepository chatMemberRepository;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null) {
                    return message;
                }

                StompCommand command = accessor.getCommand();
                if (StompCommand.CONNECT.equals(command)) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
                        log.warn("STOMP connect rejected: missing or invalid Authorization header");
                        throw new AccessDeniedException("Missing or invalid Authorization header");
                    }
                    String token = authHeader.substring(7);
                    if (!tokenProvider.validateToken(token)) {
                        log.warn("STOMP connect rejected: invalid JWT token");
                        throw new AccessDeniedException("Invalid JWT token");
                    }
                    UUID userId = tokenProvider.getUserIdFromToken(token);
                    User user = userRepository.findById(userId).orElse(null);
                    if (user == null || user.getStatus() != UserStatus.ACTIVE) {
                        log.warn("STOMP connect rejected: user {} not found or not active", userId);
                        throw new AccessDeniedException("User not active or does not exist");
                    }

                    UserPrincipal userPrincipal = new UserPrincipal(
                            user.getId(),
                            user.getEmail(),
                            "",
                            user.getRole(),
                            user.getStatus()
                    );
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
                    accessor.setUser(auth);
                    log.debug("STOMP connect authenticated user: {}", userId);
                } else if (StompCommand.SUBSCRIBE.equals(command)) {
                    Principal principal = accessor.getUser();
                    if (principal == null) {
                        log.warn("STOMP subscribe rejected: unauthenticated session");
                        throw new AccessDeniedException("Unauthenticated STOMP subscription");
                    }
                    String destination = accessor.getDestination();
                    if (destination != null) {
                        if (destination.startsWith("/topic/chats/")) {
                            String chatIdStr = destination.substring("/topic/chats/".length());
                            try {
                                UUID chatId = UUID.fromString(chatIdStr);
                                UUID userId = ((UserPrincipal) ((UsernamePasswordAuthenticationToken) principal).getPrincipal()).getId();
                                if (!chatMemberRepository.existsByChatIdAndUserIdAndIsActiveTrue(chatId, userId)) {
                                    log.warn("STOMP subscribe rejected: user {} is not an active participant of chat {}", userId, chatId);
                                    throw new AccessDeniedException("User is not a participant of chat " + chatId);
                                }
                            } catch (IllegalArgumentException e) {
                                log.warn("STOMP subscribe rejected: malformed chat ID {}", chatIdStr);
                                throw new AccessDeniedException("Invalid chat destination ID");
                            }
                        }
                    }
                } else if (StompCommand.SEND.equals(command)) {
                    String destination = accessor.getDestination();
                    if (destination != null && (destination.startsWith("/topic/") || destination.startsWith("/queue/"))) {
                        log.warn("STOMP direct SEND rejected to broker destination: {}", destination);
                        throw new AccessDeniedException("Direct sending to broker destinations is not allowed");
                    }
                }
                return message;
            }
        });
    }
}
