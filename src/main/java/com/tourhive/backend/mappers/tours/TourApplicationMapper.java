package com.tourhive.backend.mappers.tours;

import com.tourhive.backend.dto.tours.response.ApplicationHistoryItemResponseDto;
import com.tourhive.backend.dto.tours.response.TourApplicationResponseDto;
import com.tourhive.backend.dto.tours.response.TourApplicationShortResponseDto;
import com.tourhive.backend.entities.tours.ApplicationStatusHistory;
import com.tourhive.backend.entities.tours.Tour;
import com.tourhive.backend.entities.tours.TourApplication;
import com.tourhive.backend.entities.users.UserProfile;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TourApplicationMapper {

    default ApplicationHistoryItemResponseDto toHistoryDto(ApplicationStatusHistory history) {
        if (history == null) return null;
        return new ApplicationHistoryItemResponseDto(
                history.getId(),
                history.getOldStatus(),
                history.getNewStatus(),
                history.getChangedBy() != null ? history.getChangedBy().getId() : null,
                history.getReason(),
                history.getCreatedAt()
        );
    }

    List<ApplicationHistoryItemResponseDto> toHistoryDtoList(List<ApplicationStatusHistory> historyList);

    default TourApplicationResponseDto toResponseDto(
            TourApplication application,
            UserProfile applicantProfile,
            List<ApplicationStatusHistory> history
    ) {
        if (application == null) return null;

        String nickname = applicantProfile != null ? applicantProfile.getNickname() : null;
        String fullName = resolveFullName(applicantProfile);
        String avatarUrl = resolveAvatarUrl(applicantProfile);

        return new TourApplicationResponseDto(
                application.getId(),
                application.getTour() != null ? application.getTour().getId() : null,
                application.getTour() != null ? application.getTour().getTitle() : null,
                application.getApplicant() != null ? application.getApplicant().getId() : null,
                nickname,
                fullName,
                avatarUrl,
                application.getStatus(),
                application.getMessage(),
                application.getContactInfo(),
                application.getRejectionReason(),
                application.getCancellationReason(),
                application.getCreatedAt(),
                application.getUpdatedAt(),
                toHistoryDtoList(history)
        );
    }

    default TourApplicationShortResponseDto toShortResponseDto(
            TourApplication application,
            UserProfile applicantProfile
    ) {
        if (application == null) return null;

        String nickname = applicantProfile != null ? applicantProfile.getNickname() : null;
        String fullName = resolveFullName(applicantProfile);
        String avatarUrl = resolveAvatarUrl(applicantProfile);
        Tour tour = application.getTour();
        String coverUrl = resolveTourCover(tour);

        return new TourApplicationShortResponseDto(
                application.getId(),
                tour != null ? tour.getId() : null,
                tour != null ? tour.getTitle() : null,
                coverUrl,
                tour != null ? tour.getStartDate() : null,
                tour != null ? tour.getEndDate() : null,
                application.getApplicant() != null ? application.getApplicant().getId() : null,
                nickname,
                fullName,
                avatarUrl,
                application.getStatus(),
                application.getCreatedAt()
        );
    }

    private String resolveFullName(UserProfile profile) {
        if (profile == null) return null;
        if (profile.getFirstName() != null && profile.getLastName() != null) {
            return profile.getFirstName() + " " + profile.getLastName();
        }
        if (profile.getFirstName() != null) return profile.getFirstName();
        return profile.getNickname();
    }

    private String resolveAvatarUrl(UserProfile profile) {
        if (profile == null || profile.getAvatarResource() == null) return null;
        return "/api/v1/resources/" + profile.getAvatarResource().getId();
    }

    private String resolveTourCover(Tour tour) {
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
}
