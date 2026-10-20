package com.tourhive.backend.controllers.admin;

import com.tourhive.backend.configurations.security.UserPrincipal;
import com.tourhive.backend.dto.admin.request.BlockUserRequestDto;
import com.tourhive.backend.dto.admin.request.UpdateUserRoleRequestDto;
import com.tourhive.backend.dto.admin.response.AdminUserDetailResponseDto;
import com.tourhive.backend.dto.admin.response.AdminUserResponseDto;
import com.tourhive.backend.entities.users.Role;
import com.tourhive.backend.entities.users.UserStatus;
import com.tourhive.backend.services.admin.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Admin Users", description = "Endpoints for administrator and moderator user management")
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "Search and list users", description = "Returns page of users with search, role and status filter", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    public ResponseEntity<com.tourhive.backend.dto.common.ApiResponseDto<Page<AdminUserResponseDto>>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) UserStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<AdminUserResponseDto> users = adminUserService.getUsers(search, role, status, pageable);
        return ResponseEntity.ok(com.tourhive.backend.dto.common.ApiResponseDto.success(users));
    }

    @Operation(summary = "Get user details (Admin)", description = "Returns detailed statistics and information about a user", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{userId}")
    public ResponseEntity<com.tourhive.backend.dto.common.ApiResponseDto<AdminUserDetailResponseDto>> getUserDetails(@PathVariable UUID userId) {
        AdminUserDetailResponseDto details = adminUserService.getUserDetails(userId);
        return ResponseEntity.ok(com.tourhive.backend.dto.common.ApiResponseDto.success(details));
    }

    @Operation(summary = "Block user account", description = "Blocks a user account, revokes all tokens and sets block reason", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{userId}/block")
    public ResponseEntity<com.tourhive.backend.dto.common.ApiResponseDto<AdminUserResponseDto>> blockUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody BlockUserRequestDto request,
            HttpServletRequest servletRequest
    ) {
        String ipAddress = servletRequest.getRemoteAddr();
        AdminUserResponseDto blocked = adminUserService.blockUser(principal.getId(), principal.getUsername(), userId, request, ipAddress);
        return ResponseEntity.ok(com.tourhive.backend.dto.common.ApiResponseDto.success("User blocked successfully", blocked));
    }

    @Operation(summary = "Unblock user account", description = "Restores user account status to ACTIVE", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{userId}/unblock")
    public ResponseEntity<com.tourhive.backend.dto.common.ApiResponseDto<AdminUserResponseDto>> unblockUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        String ipAddress = servletRequest.getRemoteAddr();
        AdminUserResponseDto unblocked = adminUserService.unblockUser(principal.getId(), principal.getUsername(), userId, ipAddress);
        return ResponseEntity.ok(com.tourhive.backend.dto.common.ApiResponseDto.success("User unblocked successfully", unblocked));
    }

    @Operation(summary = "Change user role", description = "Updates user role (Only ADMIN permitted)", security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.tourhive.backend.dto.common.ApiResponseDto<AdminUserResponseDto>> updateUserRole(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateUserRoleRequestDto request,
            HttpServletRequest servletRequest
    ) {
        String ipAddress = servletRequest.getRemoteAddr();
        AdminUserResponseDto updated = adminUserService.updateUserRole(principal.getId(), principal.getUsername(), userId, request, ipAddress);
        return ResponseEntity.ok(com.tourhive.backend.dto.common.ApiResponseDto.success("User role updated successfully", updated));
    }
}
