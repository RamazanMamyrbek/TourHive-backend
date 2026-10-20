package com.tourhive.backend.mappers.audit;

import com.tourhive.backend.dto.admin.response.AuditLogResponseDto;
import com.tourhive.backend.entities.audit.AuditLog;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    AuditLogResponseDto toDto(AuditLog auditLog);
}
