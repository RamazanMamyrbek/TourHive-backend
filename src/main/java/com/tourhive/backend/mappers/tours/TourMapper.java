package com.tourhive.backend.mappers.tours;

import com.tourhive.backend.dto.tours.response.TourImageResponseDto;
import com.tourhive.backend.dto.tours.response.TourResponseDto;
import com.tourhive.backend.dto.tours.response.TourRouteItemResponseDto;
import com.tourhive.backend.dto.tours.response.TourShortResponseDto;
import com.tourhive.backend.entities.tours.Tour;
import com.tourhive.backend.entities.tours.TourImage;
import com.tourhive.backend.entities.tours.TourRouteItem;
import com.tourhive.backend.entities.users.UserProfile;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TourMapper {

    TourRouteItemResponseDto toRouteItemDto(TourRouteItem item);

    List<TourRouteItemResponseDto> toRouteItemDtoList(List<TourRouteItem> items);

    default TourImageResponseDto toImageDto(TourImage image) {
        if (image == null) return null;
        String url = image.getResource() != null
                ? "/api/v1/resources/" + image.getResource().getId()
                : null;
        return new TourImageResponseDto(
                image.getId(),
                image.getResource() != null ? image.getResource().getId() : null,
                url,
                image.getIsCover(),
                image.getOrderIndex()
        );
    }

    List<TourImageResponseDto> toImageDtoList(List<TourImage> images);

    default String resolveCoverImageUrl(Tour tour) {
        if (tour == null || tour.getImages() == null || tour.getImages().isEmpty()) {
            return null;
        }
        return tour.getImages().stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsCover()))
                .findFirst()
                .or(() -> tour.getImages().stream().findFirst())
                .filter(img -> img.getResource() != null)
                .map(img -> "/api/v1/resources/" + img.getResource().getId())
                .orElse(null);
    }

    default String resolveOrganizerName(Tour tour, UserProfile profile) {
        if (profile != null) {
            if (profile.getFirstName() != null && profile.getLastName() != null) {
                return profile.getFirstName() + " " + profile.getLastName();
            }
            if (profile.getNickname() != null) {
                return profile.getNickname();
            }
        }
        return tour.getOrganizer() != null ? tour.getOrganizer().getEmail() : "Organizer";
    }

    default String resolveOrganizerAvatarUrl(UserProfile profile) {
        return profile != null ? profile.getAvatarUrl() : null;
    }

    default List<String> copyList(List<String> list) {
        return list != null ? new java.util.ArrayList<>(list) : java.util.Collections.emptyList();
    }

    default TourResponseDto toResponseDto(Tour tour, UserProfile profile) {
        if (tour == null) return null;
        return new TourResponseDto(
                tour.getId(),
                tour.getOrganizer() != null ? tour.getOrganizer().getId() : null,
                resolveOrganizerName(tour, profile),
                resolveOrganizerAvatarUrl(profile),
                tour.getTitle(),
                tour.getDescription(),
                tour.getTourType(),
                tour.getStatus(),
                tour.getStartDate(),
                tour.getEndDate(),
                tour.getCapacity(),
                tour.getAvailablePlaces(),
                tour.getPrice(),
                tour.getCurrency(),
                copyList(tour.getCountries()),
                copyList(tour.getCities()),
                copyList(tour.getIncludes()),
                toRouteItemDtoList(tour.getRouteItems()),
                toImageDtoList(tour.getImages()),
                resolveCoverImageUrl(tour),
                tour.getRejectionReason(),
                tour.getCreatedAt(),
                tour.getUpdatedAt()
        );
    }

    default TourShortResponseDto toShortResponseDto(Tour tour, UserProfile profile) {
        if (tour == null) return null;
        return new TourShortResponseDto(
                tour.getId(),
                tour.getOrganizer() != null ? tour.getOrganizer().getId() : null,
                resolveOrganizerName(tour, profile),
                resolveOrganizerAvatarUrl(profile),
                tour.getTitle(),
                tour.getTourType(),
                tour.getStatus(),
                tour.getStartDate(),
                tour.getEndDate(),
                tour.getCapacity(),
                tour.getAvailablePlaces(),
                tour.getPrice(),
                tour.getCurrency(),
                copyList(tour.getCountries()),
                copyList(tour.getCities()),
                resolveCoverImageUrl(tour)
        );
    }
}
