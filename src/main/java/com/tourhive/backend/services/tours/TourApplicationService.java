package com.tourhive.backend.services.tours;

import com.tourhive.backend.dto.tours.request.ApplyTourRequestDto;
import com.tourhive.backend.dto.tours.request.CancelApplicationRequestDto;
import com.tourhive.backend.dto.tours.request.ReviewApplicationRequestDto;
import com.tourhive.backend.dto.tours.response.TourApplicationResponseDto;
import com.tourhive.backend.dto.tours.response.TourApplicationShortResponseDto;
import com.tourhive.backend.entities.tours.TourApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TourApplicationService {

    TourApplicationResponseDto applyForTour(UUID tourId, UUID applicantId, ApplyTourRequestDto requestDto);

    TourApplicationResponseDto reviewApplication(UUID tourId, UUID applicationId, UUID organizerId, ReviewApplicationRequestDto requestDto);

    TourApplicationResponseDto cancelApplication(UUID tourId, UUID applicationId, UUID currentUserId, CancelApplicationRequestDto requestDto);

    TourApplicationResponseDto getApplicationById(UUID tourId, UUID applicationId, UUID currentUserId);

    Page<TourApplicationShortResponseDto> getTourApplications(UUID tourId, UUID currentUserId, TourApplicationStatus statusFilter, Pageable pageable);

    Page<TourApplicationShortResponseDto> getMyApplications(UUID applicantId, TourApplicationStatus statusFilter, Pageable pageable);
}
