package com.tourhive.backend.mappers.reports;

import com.tourhive.backend.dto.reports.response.ReportDetailResponseDto;
import com.tourhive.backend.dto.reports.response.ReportResponseDto;
import com.tourhive.backend.entities.reports.Report;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReportMapper {

    ReportResponseDto toDto(Report report);

    @Mapping(target = "reporterEmail", source = "reporterEmail")
    @Mapping(target = "targetSummary", source = "targetSummary")
    ReportDetailResponseDto toDetailDto(Report report, String reporterEmail, String targetSummary);
}
