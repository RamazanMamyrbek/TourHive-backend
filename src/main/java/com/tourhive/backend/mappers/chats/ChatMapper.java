package com.tourhive.backend.mappers.chats;

import com.tourhive.backend.dto.chats.response.ChatDetailsResponseDto;
import com.tourhive.backend.dto.chats.response.ChatMemberResponseDto;
import com.tourhive.backend.dto.chats.response.ChatMessageResponseDto;
import com.tourhive.backend.dto.chats.response.ChatRecipientDto;
import com.tourhive.backend.dto.chats.response.ChatResponseDto;
import com.tourhive.backend.entities.chats.Chat;
import com.tourhive.backend.entities.chats.ChatMember;
import com.tourhive.backend.entities.chats.Message;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserProfile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ChatMapper {

    public ChatRecipientDto toRecipientDto(User user, UserProfile profile) {
        if (user == null) {
            return null;
        }
        String fullName = profile != null ? profile.getFullName() : null;
        String nickname = profile != null ? profile.getNickname() : null;
        String avatarUrl = profile != null ? profile.getAvatarUrl() : null;

        return new ChatRecipientDto(user.getId(), fullName, nickname, avatarUrl);
    }

    public ChatMemberResponseDto toMemberResponseDto(ChatMember member, UserProfile profile) {
        if (member == null) {
            return null;
        }
        String fullName = profile != null ? profile.getFullName() : null;
        String nickname = profile != null ? profile.getNickname() : null;
        String avatarUrl = profile != null ? profile.getAvatarUrl() : null;

        return new ChatMemberResponseDto(
                member.getId(),
                member.getUser().getId(),
                fullName,
                nickname,
                avatarUrl,
                member.getRole(),
                member.isActive(),
                member.getJoinedAt(),
                member.getLeftAt()
        );
    }

    public ChatMessageResponseDto toMessageResponseDto(
            Message message,
            UserProfile senderProfile,
            boolean isMine,
            int readCount,
            boolean isRead
    ) {
        if (message == null) {
            return null;
        }
        String senderName = senderProfile != null ? senderProfile.getFullName() : null;
        String senderNickname = senderProfile != null ? senderProfile.getNickname() : null;
        String senderAvatarUrl = senderProfile != null ? senderProfile.getAvatarUrl() : null;

        return new ChatMessageResponseDto(
                message.getId(),
                message.getChat().getId(),
                message.getSender().getId(),
                senderName,
                senderNickname,
                senderAvatarUrl,
                message.getMessageType(),
                message.getContent(),
                message.getAttachmentUrl(),
                isMine,
                readCount,
                isRead,
                message.getCreatedAt()
        );
    }

    public ChatResponseDto toChatResponseDto(
            Chat chat,
            String displayTitle,
            String displayAvatarUrl,
            String tourTitle,
            ChatMessageResponseDto lastMessage,
            int unreadCount,
            int membersCount,
            ChatRecipientDto recipient
    ) {
        if (chat == null) {
            return null;
        }
        return new ChatResponseDto(
                chat.getId(),
                chat.getType(),
                displayTitle,
                displayAvatarUrl,
                chat.getTour() != null ? chat.getTour().getId() : null,
                tourTitle,
                lastMessage,
                unreadCount,
                membersCount,
                recipient,
                chat.getUpdatedAt()
        );
    }

    public ChatDetailsResponseDto toChatDetailsResponseDto(
            Chat chat,
            String displayTitle,
            String displayAvatarUrl,
            String tourTitle,
            List<ChatMemberResponseDto> members
    ) {
        if (chat == null) {
            return null;
        }
        return new ChatDetailsResponseDto(
                chat.getId(),
                chat.getType(),
                displayTitle,
                displayAvatarUrl,
                chat.getTour() != null ? chat.getTour().getId() : null,
                tourTitle,
                members,
                chat.getCreatedAt(),
                chat.getUpdatedAt()
        );
    }
}
