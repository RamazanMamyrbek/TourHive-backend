package com.tourhive.backend.services.tours;

import com.tourhive.backend.dto.tours.request.AddTourImageRequestDto;
import com.tourhive.backend.dto.tours.request.CreateTourRequestDto;
import com.tourhive.backend.dto.tours.request.UpdateTourRequestDto;
import com.tourhive.backend.dto.tours.response.TourImageResponseDto;
import com.tourhive.backend.dto.tours.response.TourResponseDto;
import com.tourhive.backend.dto.tours.response.TourShortResponseDto;

import java.util.List;
import java.util.UUID;

public interface TourService {

    TourResponseDto createDraft(UUID organizerId, CreateTourRequestDto requestDto);

    TourResponseDto updateDraft(UUID tourId, UUID organizerId, UpdateTourRequestDto requestDto);

    List<TourShortResponseDto> getMyDrafts(UUID organizerId);

    TourResponseDto getTourById(UUID tourId, UUID currentUserId);

    void deleteTour(UUID tourId, UUID organizerId);

    TourImageResponseDto addTourImage(UUID tourId, UUID organizerId, AddTourImageRequestDto requestDto);

    void removeTourImage(UUID tourId, UUID imageId, UUID organizerId);

    org.springframework.data.domain.Page<TourShortResponseDto> searchPublishedTours(
            com.tourhive.backend.dto.tours.request.TourSearchCriteria criteria,
            org.springframework.data.domain.Pageable pageable
    );

    TourResponseDto publishTour(UUID tourId, UUID organizerId);

    TourResponseDto cancelTour(UUID tourId, UUID organizerId, com.tourhive.backend.dto.tours.request.CancelTourRequestDto requestDto);
}
