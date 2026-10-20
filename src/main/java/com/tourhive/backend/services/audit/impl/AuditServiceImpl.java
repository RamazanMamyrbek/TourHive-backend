package com.tourhive.backend.services.audit.impl;

import com.tourhive.backend.dto.admin.response.AuditLogResponseDto;
import com.tourhive.backend.entities.audit.AuditAction;
import com.tourhive.backend.entities.audit.AuditLog;
import com.tourhive.backend.mappers.audit.AuditLogMapper;
import com.tourhive.backend.repositories.audit.AuditLogRepository;
import com.tourhive.backend.services.audit.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(UUID actorId, String actorEmail, AuditAction action, String targetType, UUID targetId, String details, String ipAddress) {
        AuditLog auditLog = AuditLog.builder()
                .actorId(actorId)
                .actorEmail(actorEmail != null ? actorEmail : "system")
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .details(details)
                .ipAddress(ipAddress)
                .createdAt(Instant.now())
                .build();

        auditLogRepository.save(auditLog);
        log.info("AUDIT LOG: actor={} action={} target={}:{} details={}", actorEmail, action, targetType, targetId, details);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponseDto> getAuditLogs(AuditAction action, String targetType, UUID actorId, Pageable pageable) {
        Page<AuditLog> page;
        if (action != null) {
            page = auditLogRepository.findAllByAction(action, pageable);
        } else if (targetType != null) {
            page = auditLogRepository.findAllByTargetType(targetType, pageable);
        } else if (actorId != null) {
            page = auditLogRepository.findAllByActorId(actorId, pageable);
        } else {
            page = auditLogRepository.findAll(pageable);
        }
        return page.map(auditLogMapper::toDto);
    }
}
