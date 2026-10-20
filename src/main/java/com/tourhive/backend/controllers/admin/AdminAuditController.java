package com.tourhive.backend.controllers.admin;

import com.tourhive.backend.dto.admin.response.AuditLogResponseDto;
import com.tourhive.backend.dto.common.ApiResponseDto;
import com.tourhive.backend.entities.audit.AuditAction;
import com.tourhive.backend.services.audit.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Admin Audit Logs", description = "Endpoints for inspecting administrative and moderation action history")
@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
public class AdminAuditController {

    private final AuditService auditService;

    @Operation(summary = "Get audit logs", description = "Returns page of administrative actions with optional filters", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    public ResponseEntity<ApiResponseDto<Page<AuditLogResponseDto>>> getAuditLogs(
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) UUID actorId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<AuditLogResponseDto> logs = auditService.getAuditLogs(action, targetType, actorId, pageable);
        return ResponseEntity.ok(ApiResponseDto.success(logs));
    }
}
