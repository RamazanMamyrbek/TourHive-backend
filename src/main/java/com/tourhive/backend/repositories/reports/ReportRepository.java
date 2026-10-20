package com.tourhive.backend.repositories.reports;

import com.tourhive.backend.entities.reports.Report;
import com.tourhive.backend.entities.reports.ReportStatus;
import com.tourhive.backend.entities.reports.ReportTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {

    boolean existsByReporterIdAndTargetTypeAndTargetIdAndStatusIn(
            UUID reporterId,
            ReportTargetType targetType,
            UUID targetId,
            Collection<ReportStatus> statuses
    );

    Page<Report> findAllByReporterId(UUID reporterId, Pageable pageable);

    Page<Report> findAllByStatus(ReportStatus status, Pageable pageable);

    Page<Report> findAllByTargetType(ReportTargetType targetType, Pageable pageable);

    Page<Report> findAllByStatusAndTargetType(ReportStatus status, ReportTargetType targetType, Pageable pageable);

    long countByReporterId(UUID reporterId);

    long countByTargetTypeAndTargetId(ReportTargetType targetType, UUID targetId);
}
