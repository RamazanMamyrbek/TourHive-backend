package com.tourhive.backend.controllers.notifications;

import com.tourhive.backend.configurations.security.UserPrincipal;
import com.tourhive.backend.dto.common.ApiResponseDto;
import com.tourhive.backend.dto.notifications.request.RegisterDeviceTokenRequestDto;
import com.tourhive.backend.dto.notifications.response.NotificationResponseDto;
import com.tourhive.backend.dto.notifications.response.UnreadCountResponseDto;
import com.tourhive.backend.services.notifications.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Notifications", description = "Endpoints for in-app notifications and device tokens")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(
            summary = "Get my notifications",
            description = "Returns paginated list of notifications for current user with optional unreadOnly filter",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping
    public ResponseEntity<ApiResponseDto<Page<NotificationResponseDto>>> getMyNotifications(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @ParameterObject Pageable pageable
    ) {
        Page<NotificationResponseDto> response = notificationService.getMyNotifications(principal.getId(), unreadOnly, pageable);
        return ResponseEntity.ok(ApiResponseDto.success(response));
    }

    @Operation(
            summary = "Get unread count",
            description = "Returns count of unread notifications for badge display",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponseDto<UnreadCountResponseDto>> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UnreadCountResponseDto response = notificationService.getUnreadCount(principal.getId());
        return ResponseEntity.ok(ApiResponseDto.success(response));
    }

    @Operation(
            summary = "Mark notification as read",
            description = "Marks a specific notification as read",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponseDto<Void>> markAsRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        notificationService.markAsRead(id, principal.getId());
        return ResponseEntity.ok(ApiResponseDto.success("Notification marked as read", null));
    }

    @Operation(
            summary = "Mark all notifications as read",
            description = "Marks all unread notifications of current user as read",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponseDto<Void>> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        notificationService.markAllAsRead(principal.getId());
        return ResponseEntity.ok(ApiResponseDto.success("All notifications marked as read", null));
    }

    @Operation(
            summary = "Register device token",
            description = "Registers or updates mobile device FCM push token",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/device-token")
    public ResponseEntity<ApiResponseDto<Void>> registerDeviceToken(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RegisterDeviceTokenRequestDto requestDto
    ) {
        notificationService.registerDeviceToken(principal.getId(), requestDto);
        return ResponseEntity.ok(ApiResponseDto.success("Device token registered successfully", null));
    }

    @Operation(
            summary = "Remove device token",
            description = "Removes specific device token or all tokens for user on logout",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/device-token")
    public ResponseEntity<ApiResponseDto<Void>> removeDeviceToken(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String token
    ) {
        notificationService.removeDeviceToken(principal.getId(), token);
        return ResponseEntity.ok(ApiResponseDto.success("Device token removed successfully", null));
    }
}
