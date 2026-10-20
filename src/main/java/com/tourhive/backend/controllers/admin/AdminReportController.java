package com.tourhive.backend.controllers.admin;

import com.tourhive.backend.configurations.security.UserPrincipal;
import com.tourhive.backend.dto.common.ApiResponseDto;
import com.tourhive.backend.dto.reports.request.ResolveReportRequestDto;
import com.tourhive.backend.dto.reports.response.ReportDetailResponseDto;
import com.tourhive.backend.dto.reports.response.ReportResponseDto;
import com.tourhive.backend.entities.reports.ReportStatus;
import com.tourhive.backend.entities.reports.ReportTargetType;
import com.tourhive.backend.services.reports.ReportService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Admin Reports", description = "Moderation endpoints for inspecting and resolving user complaints")
@RestController
@RequestMapping("/api/v1/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
public class AdminReportController {

    private final ReportService reportService;

    @Operation(summary = "Get reports queue", description = "Returns page of reports with optional status and targetType filter", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    public ResponseEntity<ApiResponseDto<Page<ReportResponseDto>>> getReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) ReportTargetType targetType,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ReportResponseDto> reports = reportService.getReports(status, targetType, pageable);
        return ResponseEntity.ok(ApiResponseDto.success(reports));
    }

    @Operation(summary = "Get report details", description = "Returns detailed report information with target context", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<ReportDetailResponseDto>> getReportDetails(@PathVariable UUID id) {
        ReportDetailResponseDto details = reportService.getReportDetails(id);
        return ResponseEntity.ok(ApiResponseDto.success(details));
    }

    @Operation(summary = "Resolve report", description = "Resolves or dismisses a report with moderator actions and notes", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{id}/resolve")
    public ResponseEntity<ApiResponseDto<ReportResponseDto>> resolveReport(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ResolveReportRequestDto request,
            HttpServletRequest servletRequest
    ) {
        String ipAddress = servletRequest.getRemoteAddr();
        ReportResponseDto response = reportService.resolveReport(id, principal.getId(), principal.getUsername(), request, ipAddress);
        return ResponseEntity.ok(ApiResponseDto.success("Report resolved successfully", response));
    }
}
