package com.tourhive.backend.services.organizers;

import com.tourhive.backend.dto.organizers.request.RegisterBusinessProfileRequestDto;
import com.tourhive.backend.dto.organizers.response.BusinessProfileResponseDto;
import com.tourhive.backend.dto.organizers.response.OrganizerProfileResponseDto;

import java.util.UUID;

public interface OrganizerService {

    OrganizerProfileResponseDto activateOrganizer(UUID userId);

    BusinessProfileResponseDto registerBusinessProfile(UUID userId, RegisterBusinessProfileRequestDto dto);

    OrganizerProfileResponseDto getMyOrganizerProfile(UUID userId);

    OrganizerProfileResponseDto getPublicOrganizerProfile(UUID requestingUserId, UUID targetUserId);

    BusinessProfileResponseDto getMyBusinessProfile(UUID userId);
}
