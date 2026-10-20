package com.tourhive.backend.controllers.reports;

import com.tourhive.backend.configurations.security.UserPrincipal;
import com.tourhive.backend.dto.common.ApiResponseDto;
import com.tourhive.backend.dto.reports.request.CreateReportRequestDto;
import com.tourhive.backend.dto.reports.response.ReportResponseDto;
import com.tourhive.backend.services.reports.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Reports", description = "Endpoints for user complaints / moderation reports")
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "Submit a complaint / report", description = "Allows a user to report a profile, tour, message or review", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    public ResponseEntity<ApiResponseDto<ReportResponseDto>> submitReport(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateReportRequestDto request
    ) {
        ReportResponseDto response = reportService.createReport(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("Report submitted successfully", response));
    }

    @Operation(summary = "Get my submitted reports", description = "Returns page of reports submitted by current user", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/my")
    public ResponseEntity<ApiResponseDto<Page<ReportResponseDto>>> getMyReports(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ReportResponseDto> reports = reportService.getMyReports(principal.getId(), pageable);
        return ResponseEntity.ok(ApiResponseDto.success(reports));
    }
}
