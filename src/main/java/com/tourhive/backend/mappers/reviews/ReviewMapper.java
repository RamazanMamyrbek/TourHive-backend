package com.tourhive.backend.mappers.reviews;

import com.tourhive.backend.dto.reviews.response.ReviewResponseDto;
import com.tourhive.backend.entities.reviews.Review;
import com.tourhive.backend.entities.users.UserProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    @Mapping(target = "id", source = "review.id")
    @Mapping(target = "tourId", source = "review.tour.id")
    @Mapping(target = "tourTitle", source = "review.tour.title")
    @Mapping(target = "organizerId", source = "review.organizer.id")
    @Mapping(target = "organizerName", expression = "java(review.getOrganizer().getEmail())")
    @Mapping(target = "authorId", source = "review.author.id")
    @Mapping(target = "authorName", expression = "java(authorProfile != null && authorProfile.getFullName() != null ? authorProfile.getFullName() : review.getAuthor().getEmail())")
    @Mapping(target = "authorNickname", expression = "java(authorProfile != null ? authorProfile.getNickname() : null)")
    @Mapping(target = "authorAvatarUrl", expression = "java(authorProfile != null ? authorProfile.getAvatarUrl() : null)")
    @Mapping(target = "rating", source = "review.rating")
    @Mapping(target = "comment", source = "review.comment")
    @Mapping(target = "createdAt", source = "review.createdAt")
    @Mapping(target = "updatedAt", source = "review.updatedAt")
    ReviewResponseDto toDto(Review review, UserProfile authorProfile);
}
