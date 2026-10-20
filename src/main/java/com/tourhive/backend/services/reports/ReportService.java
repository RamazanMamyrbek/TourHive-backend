package com.tourhive.backend.services.reports;

import com.tourhive.backend.dto.reports.request.CreateReportRequestDto;
import com.tourhive.backend.dto.reports.request.ResolveReportRequestDto;
import com.tourhive.backend.dto.reports.response.ReportDetailResponseDto;
import com.tourhive.backend.dto.reports.response.ReportResponseDto;
import com.tourhive.backend.entities.reports.ReportStatus;
import com.tourhive.backend.entities.reports.ReportTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ReportService {

    ReportResponseDto createReport(UUID reporterId, CreateReportRequestDto request);

    Page<ReportResponseDto> getMyReports(UUID reporterId, Pageable pageable);

    Page<ReportResponseDto> getReports(ReportStatus status, ReportTargetType targetType, Pageable pageable);

    ReportDetailResponseDto getReportDetails(UUID reportId);

    ReportResponseDto resolveReport(UUID reportId, UUID resolverId, String resolverEmail, ResolveReportRequestDto request, String ipAddress);
}
