package com.tourhive.backend.dto.reports.request;

import com.tourhive.backend.entities.reports.ReportActionTaken;
import com.tourhive.backend.entities.reports.ReportStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to resolve or dismiss a complaint / report")
public record ResolveReportRequestDto(
        @NotNull(message = "Resolution status is required (RESOLVED or DISMISSED)")
        @Schema(description = "Target status (RESOLVED or DISMISSED)", example = "RESOLVED")
        ReportStatus status,

        @Schema(description = "Action taken if resolved", example = "USER_BLOCKED")
        ReportActionTaken actionTaken,

        @Size(max = 2000, message = "Resolution notes cannot exceed 2000 characters")
        @Schema(description = "Moderator comments regarding the resolution", example = "Confirmed violation of Terms of Service, user blocked.")
        String resolutionNotes
) {
}
