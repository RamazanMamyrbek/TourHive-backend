package com.tourhive.backend.services.admin;

import com.tourhive.backend.dto.admin.request.ReviewTourRequestDto;
import com.tourhive.backend.dto.tours.response.TourResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AdminTourService {

    Page<TourResponseDto> getPendingTours(Pageable pageable);

    TourResponseDto reviewTour(UUID reviewerId, UUID tourId, ReviewTourRequestDto request);
}
