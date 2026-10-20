package com.tourhive.backend.dto.reports.request;

import com.tourhive.backend.entities.reports.ReportReason;
import com.tourhive.backend.entities.reports.ReportTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "Request to submit a complaint / report")
public record CreateReportRequestDto(
        @NotNull(message = "Target type is required")
        @Schema(description = "Type of the reported entity (USER, TOUR, MESSAGE, REVIEW)", example = "TOUR")
        ReportTargetType targetType,

        @NotNull(message = "Target ID is required")
        @Schema(description = "UUID of the reported entity", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID targetId,

        @NotNull(message = "Reason is required")
        @Schema(description = "Violation reason", example = "SPAM")
        ReportReason reason,

        @NotBlank(message = "Description cannot be blank")
        @Size(max = 1000, message = "Description cannot exceed 1000 characters")
        @Schema(description = "Detailed explanation of the report", example = "Off-platform commercial payment requests in chat")
        String description
) {
}
