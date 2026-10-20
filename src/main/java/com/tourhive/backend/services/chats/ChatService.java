package com.tourhive.backend.services.chats;

import com.tourhive.backend.dto.chats.request.CreateDirectChatRequestDto;
import com.tourhive.backend.dto.chats.request.MarkMessagesReadRequestDto;
import com.tourhive.backend.dto.chats.request.SendMessageRequestDto;
import com.tourhive.backend.dto.chats.response.ChatDetailsResponseDto;
import com.tourhive.backend.dto.chats.response.ChatMessageResponseDto;
import com.tourhive.backend.dto.chats.response.ChatResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ChatService {

    ChatResponseDto getOrCreateDirectChat(UUID currentUserId, CreateDirectChatRequestDto requestDto);

    ChatResponseDto getOrCreateTourGroupChat(UUID tourId, UUID currentUserId);

    Page<ChatResponseDto> getMyChats(UUID currentUserId, Pageable pageable);

    ChatDetailsResponseDto getChatDetails(UUID chatId, UUID currentUserId);

    ChatMessageResponseDto sendMessage(UUID chatId, UUID currentUserId, SendMessageRequestDto requestDto);

    Page<ChatMessageResponseDto> getChatMessages(UUID chatId, UUID currentUserId, Pageable pageable);

    void markMessagesAsRead(UUID chatId, UUID currentUserId, MarkMessagesReadRequestDto requestDto);

    void addTourParticipantToChat(UUID tourId, UUID participantId);

    void removeTourParticipantFromChat(UUID tourId, UUID participantId);

    void deactivateChatForCancelledTour(UUID tourId);
}
