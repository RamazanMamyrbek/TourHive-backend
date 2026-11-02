package com.tourhive.backend.mappers.users;

import com.tourhive.backend.dto.auth.response.UserSummaryResponseDto;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserProfile;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    default UserSummaryResponseDto toSummaryDto(User user) {
        return toSummaryDto(user, null);
    }

    default UserSummaryResponseDto toSummaryDto(User user, UserProfile profile) {
        if (user == null) {
            return null;
        }
        return new UserSummaryResponseDto(
                user.getId(),
                user.getEmail(),
                profile != null ? profile.getFirstName() : null,
                profile != null ? profile.getLastName() : null,
                profile != null ? profile.getAvatarUrl() : null,
                user.getRole(),
                user.getStatus(),
                user.isOrganizer(),
                user.isBusinessVerified(),
                user.getCreatedAt()
        );
    }
}
