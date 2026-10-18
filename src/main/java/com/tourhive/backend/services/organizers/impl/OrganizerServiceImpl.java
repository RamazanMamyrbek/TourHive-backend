package com.tourhive.backend.services.organizers.impl;

import com.tourhive.backend.dto.organizers.request.RegisterBusinessProfileRequestDto;
import com.tourhive.backend.dto.organizers.response.BusinessProfileResponseDto;
import com.tourhive.backend.dto.organizers.response.OrganizerProfileResponseDto;
import com.tourhive.backend.entities.organizers.BusinessDocument;
import com.tourhive.backend.entities.organizers.BusinessProfile;
import com.tourhive.backend.entities.organizers.BusinessVerificationStatus;
import com.tourhive.backend.entities.organizers.OrganizerProfile;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.exceptions.BusinessProfileAlreadyExistsException;
import com.tourhive.backend.exceptions.ResourceNotFoundException;
import com.tourhive.backend.mappers.organizers.OrganizerMapper;
import com.tourhive.backend.repositories.organizers.BusinessProfileRepository;
import com.tourhive.backend.repositories.organizers.OrganizerProfileRepository;
import com.tourhive.backend.repositories.users.UserBlockRepository;
import com.tourhive.backend.repositories.users.UserRepository;
import com.tourhive.backend.services.organizers.OrganizerService;
import com.tourhive.backend.services.users.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizerServiceImpl implements OrganizerService {

    private final OrganizerProfileRepository organizerProfileRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final UserBlockRepository userBlockRepository;
    private final OrganizerMapper organizerMapper;

    @Override
    @Transactional
    public OrganizerProfileResponseDto activateOrganizer(UUID userId) {
        User user = userService.getById(userId);
        if (!user.isOrganizer()) {
            user.setOrganizer(true);
            userRepository.save(user);
            log.info("Activated organizer status for user: {}", userId);
        }

        OrganizerProfile profile = organizerProfileRepository.findByUserId(userId)
                .orElseGet(() -> organizerProfileRepository.save(
                        OrganizerProfile.builder()
                                .user(user)
                                .experienceYears(0)
                                .rating(BigDecimal.ZERO)
                                .reviewsCount(0)
                                .build()
                ));

        BusinessProfile businessProfile = businessProfileRepository.findByUserId(userId).orElse(null);
        BusinessProfileResponseDto businessDto = businessProfile != null
                ? organizerMapper.toBusinessProfileDto(businessProfile)
                : null;

        return organizerMapper.toOrganizerProfileDto(profile, businessDto);
    }

    @Override
    @Transactional
    public BusinessProfileResponseDto registerBusinessProfile(UUID userId, RegisterBusinessProfileRequestDto dto) {
        User user = userService.getById(userId);

        boolean hasActive = businessProfileRepository.existsByUserIdAndStatusIn(
                userId,
                List.of(BusinessVerificationStatus.PENDING, BusinessVerificationStatus.VERIFIED)
        );
        if (hasActive) {
            throw new BusinessProfileAlreadyExistsException("Active or pending business profile already exists");
        }

        if (!user.isOrganizer()) {
            user.setOrganizer(true);
            userRepository.save(user);
        }

        if (organizerProfileRepository.findByUserId(userId).isEmpty()) {
            organizerProfileRepository.save(
                    OrganizerProfile.builder()
                            .user(user)
                            .experienceYears(0)
                            .rating(BigDecimal.ZERO)
                            .reviewsCount(0)
                            .build()
            );
        }

        BusinessProfile profile = businessProfileRepository.findByUserId(userId)
                .orElseGet(() -> BusinessProfile.builder().user(user).build());

        profile.setLegalName(dto.legalName());
        profile.setRegistrationNumber(dto.registrationNumber());
        profile.setCountry(dto.country());
        profile.setContactEmail(dto.contactEmail());
        profile.setContactPhone(dto.contactPhone());
        profile.setDescription(dto.description());
        profile.setStatus(BusinessVerificationStatus.PENDING);
        profile.setRejectionReason(null);
        profile.setReviewedBy(null);
        profile.setReviewedAt(null);
        profile.getDocuments().clear();

        if (dto.documents() != null) {
            for (var docDto : dto.documents()) {
                profile.addDocument(BusinessDocument.builder()
                        .documentName(docDto.documentName())
                        .documentUrl(docDto.documentUrl())
                        .build());
            }
        }

        BusinessProfile saved = businessProfileRepository.save(profile);
        log.info("Registered business profile application for user: {}", userId);
        return organizerMapper.toBusinessProfileDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizerProfileResponseDto getMyOrganizerProfile(UUID userId) {
        OrganizerProfile profile = organizerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Organizer profile not found for user: " + userId));

        BusinessProfile businessProfile = businessProfileRepository.findByUserId(userId).orElse(null);
        BusinessProfileResponseDto businessDto = businessProfile != null
                ? organizerMapper.toBusinessProfileDto(businessProfile)
                : null;

        return organizerMapper.toOrganizerProfileDto(profile, businessDto);
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizerProfileResponseDto getPublicOrganizerProfile(UUID requestingUserId, UUID targetUserId) {
        if (requestingUserId != null && userBlockRepository.isBlockedBetween(requestingUserId, targetUserId)) {
            throw new ResourceNotFoundException("Organizer not found: " + targetUserId);
        }

        User targetUser = userService.getById(targetUserId);
        if (!targetUser.isOrganizer()) {
            throw new ResourceNotFoundException("User is not an organizer: " + targetUserId);
        }

        OrganizerProfile profile = organizerProfileRepository.findByUserId(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Organizer profile not found: " + targetUserId));

        BusinessProfile businessProfile = businessProfileRepository.findByUserId(targetUserId).orElse(null);
        BusinessProfileResponseDto businessDto = (businessProfile != null && businessProfile.getStatus() == BusinessVerificationStatus.VERIFIED)
                ? organizerMapper.toBusinessProfileDto(businessProfile)
                : null;

        return organizerMapper.toOrganizerProfileDto(profile, businessDto);
    }

    @Override
    @Transactional(readOnly = true)
    public BusinessProfileResponseDto getMyBusinessProfile(UUID userId) {
        BusinessProfile profile = businessProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Business profile not found for user: " + userId));

        return organizerMapper.toBusinessProfileDto(profile);
    }
}
