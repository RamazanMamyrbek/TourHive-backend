package com.tourhive.backend.services.admin;

import com.tourhive.backend.dto.admin.request.ReviewBusinessProfileRequestDto;
import com.tourhive.backend.dto.admin.response.BusinessProfileAdminResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AdminVerificationService {

    Page<BusinessProfileAdminResponseDto> getPendingVerifications(Pageable pageable);

    BusinessProfileAdminResponseDto getVerificationDetails(UUID profileId);

    BusinessProfileAdminResponseDto reviewBusinessProfile(UUID reviewerId, UUID profileId, ReviewBusinessProfileRequestDto dto);
}
