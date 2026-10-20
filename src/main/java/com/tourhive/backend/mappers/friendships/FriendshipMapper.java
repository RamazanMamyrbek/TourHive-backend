package com.tourhive.backend.mappers.friendships;

import com.tourhive.backend.dto.friendships.response.FriendRequestResponseDto;
import com.tourhive.backend.dto.friendships.response.FriendSummaryResponseDto;
import com.tourhive.backend.entities.friendships.FriendRequest;
import com.tourhive.backend.entities.friendships.Friendship;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FriendshipMapper {

    @Mapping(target = "id", source = "request.id")
    @Mapping(target = "senderId", source = "request.sender.id")
    @Mapping(target = "senderEmail", source = "request.sender.email")
    @Mapping(target = "senderNickname", source = "senderProfile.nickname")
    @Mapping(target = "senderAvatarUrl", source = "senderProfile.avatarUrl")
    @Mapping(target = "receiverId", source = "request.receiver.id")
    @Mapping(target = "receiverEmail", source = "request.receiver.email")
    @Mapping(target = "receiverNickname", source = "receiverProfile.nickname")
    @Mapping(target = "receiverAvatarUrl", source = "receiverProfile.avatarUrl")
    @Mapping(target = "status", source = "request.status")
    @Mapping(target = "message", source = "request.message")
    @Mapping(target = "createdAt", source = "request.createdAt")
    @Mapping(target = "updatedAt", source = "request.updatedAt")
    FriendRequestResponseDto toFriendRequestResponseDto(FriendRequest request, UserProfile senderProfile, UserProfile receiverProfile);

    @Mapping(target = "userId", source = "friend.id")
    @Mapping(target = "email", source = "friend.email")
    @Mapping(target = "nickname", source = "friendProfile.nickname")
    @Mapping(target = "firstName", source = "friendProfile.firstName")
    @Mapping(target = "lastName", source = "friendProfile.lastName")
    @Mapping(target = "avatarUrl", source = "friendProfile.avatarUrl")
    @Mapping(target = "city", source = "friendProfile.city")
    @Mapping(target = "country", source = "friendProfile.country")
    @Mapping(target = "friendshipSince", source = "friendship.createdAt")
    FriendSummaryResponseDto toFriendSummaryResponseDto(Friendship friendship, User friend, UserProfile friendProfile);
}
