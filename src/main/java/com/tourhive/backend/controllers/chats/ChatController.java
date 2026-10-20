package com.tourhive.backend.controllers.chats;

import com.tourhive.backend.configurations.security.UserPrincipal;
import com.tourhive.backend.dto.chats.request.CreateDirectChatRequestDto;
import com.tourhive.backend.dto.chats.request.MarkMessagesReadRequestDto;
import com.tourhive.backend.dto.chats.request.SendMessageRequestDto;
import com.tourhive.backend.dto.chats.response.ChatDetailsResponseDto;
import com.tourhive.backend.dto.chats.response.ChatMessageResponseDto;
import com.tourhive.backend.dto.chats.response.ChatResponseDto;
import com.tourhive.backend.dto.common.ApiResponseDto;
import com.tourhive.backend.services.chats.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Chats", description = "Endpoints for direct and tour group chats, messaging, and read tracking")
@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Operation(
            summary = "Get or create direct chat",
            description = "Creates a direct 1-on-1 chat between friends or tour participants and organizer, or returns existing one",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/direct")
    public ResponseEntity<ApiResponseDto<ChatResponseDto>> getOrCreateDirectChat(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateDirectChatRequestDto requestDto
    ) {
        ChatResponseDto response = chatService.getOrCreateDirectChat(principal.getId(), requestDto);
        return ResponseEntity.ok(ApiResponseDto.success(response));
    }

    @Operation(
            summary = "Get or access tour group chat",
            description = "Accesses the group chat for a tour for organizer and accepted participants",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/tours/{tourId}")
    public ResponseEntity<ApiResponseDto<ChatResponseDto>> getOrCreateTourGroupChat(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID tourId
    ) {
        ChatResponseDto response = chatService.getOrCreateTourGroupChat(tourId, principal.getId());
        return ResponseEntity.ok(ApiResponseDto.success(response));
    }

    @Operation(
            summary = "Get my chats",
            description = "Returns paginated list of chats where current user is a member, sorted by recent activity",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping
    public ResponseEntity<ApiResponseDto<Page<ChatResponseDto>>> getMyChats(
            @AuthenticationPrincipal UserPrincipal principal,
            @ParameterObject Pageable pageable
    ) {
        Page<ChatResponseDto> response = chatService.getMyChats(principal.getId(), pageable);
        return ResponseEntity.ok(ApiResponseDto.success(response));
    }

    @Operation(
            summary = "Get chat details",
            description = "Returns detailed chat information and active/past member list",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<ChatDetailsResponseDto>> getChatDetails(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        ChatDetailsResponseDto response = chatService.getChatDetails(id, principal.getId());
        return ResponseEntity.ok(ApiResponseDto.success(response));
    }

    @Operation(
            summary = "Get chat messages",
            description = "Returns paginated message history. Left members only see messages up to their leave timestamp.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/{id}/messages")
    public ResponseEntity<ApiResponseDto<Page<ChatMessageResponseDto>>> getChatMessages(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @ParameterObject Pageable pageable
    ) {
        Page<ChatMessageResponseDto> response = chatService.getChatMessages(id, principal.getId(), pageable);
        return ResponseEntity.ok(ApiResponseDto.success(response));
    }

    @Operation(
            summary = "Send a message",
            description = "Sends a new message to the chat and broadcasts it via WebSocket STOMP",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/{id}/messages")
    public ResponseEntity<ApiResponseDto<ChatMessageResponseDto>> sendMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody SendMessageRequestDto requestDto
    ) {
        ChatMessageResponseDto response = chatService.sendMessage(id, principal.getId(), requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("Message sent successfully", response));
    }

    @Operation(
            summary = "Mark messages as read",
            description = "Marks unread messages in the chat as read up to the specified message ID or all",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponseDto<Void>> markMessagesAsRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestBody(required = false) MarkMessagesReadRequestDto requestDto
    ) {
        chatService.markMessagesAsRead(id, principal.getId(), requestDto);
        return ResponseEntity.ok(ApiResponseDto.success("Messages marked as read", null));
    }
}
