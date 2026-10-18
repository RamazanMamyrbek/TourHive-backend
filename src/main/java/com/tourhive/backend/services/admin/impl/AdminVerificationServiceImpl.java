package com.tourhive.backend.services.admin.impl;

import com.tourhive.backend.dto.admin.request.ReviewBusinessProfileRequestDto;
import com.tourhive.backend.dto.admin.response.BusinessProfileAdminResponseDto;
import com.tourhive.backend.entities.organizers.BusinessProfile;
import com.tourhive.backend.entities.organizers.BusinessVerificationStatus;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.exceptions.ResourceNotFoundException;
import com.tourhive.backend.mappers.organizers.OrganizerMapper;
import com.tourhive.backend.repositories.organizers.BusinessProfileRepository;
import com.tourhive.backend.repositories.users.UserRepository;
import com.tourhive.backend.services.admin.AdminVerificationService;
import com.tourhive.backend.services.users.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
public class AdminVerificationServiceImpl implements AdminVerificationService {

    private final BusinessProfileRepository businessProfileRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final OrganizerMapper organizerMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<BusinessProfileAdminResponseDto> getPendingVerifications(Pageable pageable) {
        return businessProfileRepository.findAllByStatus(BusinessVerificationStatus.PENDING, pageable)
                .map(organizerMapper::toAdminResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public BusinessProfileAdminResponseDto getVerificationDetails(UUID profileId) {
        BusinessProfile profile = businessProfileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Business profile not found: " + profileId));
        return organizerMapper.toAdminResponseDto(profile);
    }

    @Override
    @Transactional
    public BusinessProfileAdminResponseDto reviewBusinessProfile(UUID reviewerId, UUID profileId, ReviewBusinessProfileRequestDto dto) {
        BusinessProfile profile = businessProfileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Business profile not found: " + profileId));

        User reviewer = userService.getById(reviewerId);
        profile.setStatus(dto.status());
        profile.setReviewedBy(reviewer);
        profile.setReviewedAt(Instant.now());

        if (dto.status() == BusinessVerificationStatus.REJECTED) {
            profile.setRejectionReason(dto.rejectionReason());
        } else {
            profile.setRejectionReason(null);
        }

        User user = profile.getUser();
        user.setBusinessVerified(dto.status() == BusinessVerificationStatus.VERIFIED);
        userRepository.save(user);

        BusinessProfile saved = businessProfileRepository.save(profile);
        log.info("Admin/Moderator {} reviewed business profile {}: status={}", reviewerId, profileId, dto.status());
        return organizerMapper.toAdminResponseDto(saved);
    }
}
