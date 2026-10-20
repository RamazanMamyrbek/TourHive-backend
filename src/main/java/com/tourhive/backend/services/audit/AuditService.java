package com.tourhive.backend.services.audit;

import com.tourhive.backend.dto.admin.response.AuditLogResponseDto;
import com.tourhive.backend.entities.audit.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AuditService {

    void logAction(UUID actorId, String actorEmail, AuditAction action, String targetType, UUID targetId, String details, String ipAddress);

    Page<AuditLogResponseDto> getAuditLogs(AuditAction action, String targetType, UUID actorId, Pageable pageable);
}
