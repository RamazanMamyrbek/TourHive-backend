package com.tourhive.backend.dto.admin.response;

import com.tourhive.backend.entities.audit.AuditAction;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Audit log entry response")
public record AuditLogResponseDto(
        UUID id,
        UUID actorId,
        String actorEmail,
        AuditAction action,
        String targetType,
        UUID targetId,
        String details,
        String ipAddress,
        Instant createdAt
) {
}
