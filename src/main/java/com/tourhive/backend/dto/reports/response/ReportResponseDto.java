package com.tourhive.backend.dto.reports.response;

import com.tourhive.backend.entities.reports.ReportActionTaken;
import com.tourhive.backend.entities.reports.ReportReason;
import com.tourhive.backend.entities.reports.ReportStatus;
import com.tourhive.backend.entities.reports.ReportTargetType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Report summary response")
public record ReportResponseDto(
        UUID id,
        UUID reporterId,
        ReportTargetType targetType,
        UUID targetId,
        ReportReason reason,
        String description,
        ReportStatus status,
        ReportActionTaken actionTaken,
        String resolutionNotes,
        UUID resolvedBy,
        Instant resolvedAt,
        Instant createdAt
) {
}
