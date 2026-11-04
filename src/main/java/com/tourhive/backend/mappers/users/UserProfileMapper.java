package com.tourhive.backend.mappers.users;

import com.tourhive.backend.dto.users.request.UpdateProfileRequestDto;
import com.tourhive.backend.dto.users.response.PublicUserProfileResponseDto;
import com.tourhive.backend.dto.users.response.UserProfileResponseDto;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserProfile;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserProfileMapper {

    @Mapping(target = "id", source = "profile.id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "isOrganizer", source = "user.organizer")
    @Mapping(target = "isBusinessVerified", source = "user.businessVerified")
    @Mapping(target = "followersCount", source = "followersCount")
    @Mapping(target = "followingCount", source = "followingCount")
    @Mapping(target = "createdAt", source = "profile.createdAt")
    @Mapping(target = "updatedAt", source = "profile.updatedAt")
    UserProfileResponseDto toResponseDto(UserProfile profile, User user, long followersCount, long followingCount);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "isOrganizer", source = "user.organizer")
    @Mapping(target = "isBusinessVerified", source = "user.businessVerified")
    @Mapping(target = "followersCount", source = "followersCount")
    @Mapping(target = "followingCount", source = "followingCount")
    @Mapping(target = "isFollowing", source = "isFollowing")
    PublicUserProfileResponseDto toPublicResponseDto(UserProfile profile, User user, long followersCount, long followingCount, boolean isFollowing);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateProfileFromDto(UpdateProfileRequestDto dto, @MappingTarget UserProfile profile);
}
