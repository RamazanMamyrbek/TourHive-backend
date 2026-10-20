package com.tourhive.backend.dto.reports.response;

import com.tourhive.backend.entities.reports.ReportActionTaken;
import com.tourhive.backend.entities.reports.ReportReason;
import com.tourhive.backend.entities.reports.ReportStatus;
import com.tourhive.backend.entities.reports.ReportTargetType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Detailed report response with reporter and target context")
public record ReportDetailResponseDto(
        UUID id,
        UUID reporterId,
        String reporterEmail,
        ReportTargetType targetType,
        UUID targetId,
        String targetSummary,
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
