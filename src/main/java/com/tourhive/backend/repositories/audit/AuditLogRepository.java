package com.tourhive.backend.repositories.audit;

import com.tourhive.backend.entities.audit.AuditAction;
import com.tourhive.backend.entities.audit.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findAllByAction(AuditAction action, Pageable pageable);

    Page<AuditLog> findAllByTargetType(String targetType, Pageable pageable);

    Page<AuditLog> findAllByActorId(UUID actorId, Pageable pageable);
}
