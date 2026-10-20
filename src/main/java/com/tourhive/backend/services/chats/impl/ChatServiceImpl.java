package com.tourhive.backend.services.chats.impl;

import com.tourhive.backend.dto.chats.request.CreateDirectChatRequestDto;
import com.tourhive.backend.dto.chats.request.MarkMessagesReadRequestDto;
import com.tourhive.backend.dto.chats.request.SendMessageRequestDto;
import com.tourhive.backend.dto.chats.response.ChatDetailsResponseDto;
import com.tourhive.backend.dto.chats.response.ChatMemberResponseDto;
import com.tourhive.backend.dto.chats.response.ChatMessageResponseDto;
import com.tourhive.backend.dto.chats.response.ChatRecipientDto;
import com.tourhive.backend.dto.chats.response.ChatResponseDto;
import com.tourhive.backend.entities.chats.Chat;
import com.tourhive.backend.entities.chats.ChatMember;
import com.tourhive.backend.entities.chats.ChatMemberRole;
import com.tourhive.backend.entities.chats.ChatType;
import com.tourhive.backend.entities.chats.Message;
import com.tourhive.backend.entities.chats.MessageRead;
import com.tourhive.backend.entities.chats.MessageType;
import com.tourhive.backend.entities.tours.Tour;
import com.tourhive.backend.entities.tours.TourApplication;
import com.tourhive.backend.entities.tours.TourApplicationStatus;
import com.tourhive.backend.entities.users.Role;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserProfile;
import com.tourhive.backend.exceptions.CannotChatWithSelfException;
import com.tourhive.backend.exceptions.ChatAccessDeniedException;
import com.tourhive.backend.exceptions.ChatNotFoundException;
import com.tourhive.backend.exceptions.InvalidMessageContentException;
import com.tourhive.backend.exceptions.ResourceNotFoundException;
import com.tourhive.backend.exceptions.TourNotFoundException;
import com.tourhive.backend.exceptions.UserBlockedException;
import com.tourhive.backend.mappers.chats.ChatMapper;
import com.tourhive.backend.repositories.chats.ChatMemberRepository;
import com.tourhive.backend.repositories.chats.ChatRepository;
import com.tourhive.backend.repositories.chats.MessageReadRepository;
import com.tourhive.backend.repositories.chats.MessageRepository;
import com.tourhive.backend.repositories.friendships.FriendshipRepository;
import com.tourhive.backend.repositories.tours.TourApplicationRepository;
import com.tourhive.backend.repositories.tours.TourRepository;
import com.tourhive.backend.repositories.users.UserBlockRepository;
import com.tourhive.backend.repositories.users.UserProfileRepository;
import com.tourhive.backend.repositories.users.UserRepository;
import com.tourhive.backend.services.chats.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final MessageRepository messageRepository;
    private final MessageReadRepository messageReadRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final TourRepository tourRepository;
    private final TourApplicationRepository tourApplicationRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserBlockRepository userBlockRepository;
    private final ChatMapper chatMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public ChatResponseDto getOrCreateDirectChat(UUID currentUserId, CreateDirectChatRequestDto requestDto) {
        UUID recipientId = requestDto.recipientId();
        if (currentUserId.equals(recipientId)) {
            throw new CannotChatWithSelfException("Cannot start a chat with yourself");
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUserId));
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + recipientId));

        if (userBlockRepository.isBlockedBetween(currentUserId, recipientId)) {
            throw new UserBlockedException("Cannot start chat: user is blocked");
        }

        boolean areFriends = friendshipRepository.areFriends(currentUserId, recipientId);
        boolean hasTourRelationship = tourApplicationRepository.hasTourRelationshipBetween(currentUserId, recipientId);

        if (!areFriends && !hasTourRelationship) {
            throw new ChatAccessDeniedException("Direct chat is only available between friends or tour participants and organizer");
        }

        Optional<Chat> existing = chatRepository.findDirectChatBetween(currentUserId, recipientId);
        Chat chat;
        if (existing.isPresent()) {
            chat = existing.get();
            // Ensure memberships are active
            ChatMember m1 = chatMemberRepository.findByChatIdAndUserId(chat.getId(), currentUserId).orElse(null);
            if (m1 != null && !m1.isActive()) {
                m1.setActive(true);
                m1.setLeftAt(null);
                chatMemberRepository.save(m1);
            }
            ChatMember m2 = chatMemberRepository.findByChatIdAndUserId(chat.getId(), recipientId).orElse(null);
            if (m2 != null && !m2.isActive()) {
                m2.setActive(true);
                m2.setLeftAt(null);
                chatMemberRepository.save(m2);
            }
        } else {
            chat = chatRepository.save(Chat.createDirectChat(currentUser, recipient, currentUser));
            chatMemberRepository.save(ChatMember.builder().chat(chat).user(currentUser).role(ChatMemberRole.MEMBER).build());
            chatMemberRepository.save(ChatMember.builder().chat(chat).user(recipient).role(ChatMemberRole.MEMBER).build());
            log.info("Created new direct chat between {} and {}", currentUserId, recipientId);
        }

        return mapToChatResponseDto(chat, currentUserId);
    }

    @Override
    @Transactional
    public ChatResponseDto getOrCreateTourGroupChat(UUID tourId, UUID currentUserId) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new TourNotFoundException("Tour not found: " + tourId));
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUserId));

        boolean isOrganizer = tour.getOrganizer().getId().equals(currentUserId);
        boolean isAcceptedParticipant = tourApplicationRepository.existsByTourIdAndApplicantIdAndStatus(
                tourId, currentUserId, TourApplicationStatus.ACCEPTED
        );
        boolean isStaff = currentUser.getRole() == Role.MODERATOR || currentUser.getRole() == Role.ADMIN;

        if (!isOrganizer && !isAcceptedParticipant && !isStaff) {
            throw new ChatAccessDeniedException("Only tour organizer and accepted participants can access the tour group chat");
        }

        Chat chat = chatRepository.findByTourId(tourId).orElseGet(() -> {
            Chat newChat = chatRepository.save(Chat.createTourGroupChat(tour, tour.getOrganizer()));
            chatMemberRepository.save(ChatMember.builder().chat(newChat).user(tour.getOrganizer()).role(ChatMemberRole.ORGANIZER).build());

            List<TourApplication> accepted = tourApplicationRepository.findAllByTourIdAndStatusIn(
                    tourId, List.of(TourApplicationStatus.ACCEPTED)
            );
            for (TourApplication app : accepted) {
                chatMemberRepository.save(ChatMember.builder().chat(newChat).user(app.getApplicant()).role(ChatMemberRole.MEMBER).build());
            }
            log.info("Created tour group chat for tour {}", tourId);
            return newChat;
        });

        ChatMember member = chatMemberRepository.findByChatIdAndUserId(chat.getId(), currentUserId).orElse(null);
        if (member == null) {
            ChatMemberRole role = isOrganizer ? ChatMemberRole.ORGANIZER : ChatMemberRole.MEMBER;
            chatMemberRepository.save(ChatMember.builder().chat(chat).user(currentUser).role(role).build());
        } else if (!member.isActive()) {
            member.setActive(true);
            member.setLeftAt(null);
            chatMemberRepository.save(member);
        }

        return mapToChatResponseDto(chat, currentUserId);
    }

    @Override
    public Page<ChatResponseDto> getMyChats(UUID currentUserId, Pageable pageable) {
        Page<Chat> page = chatRepository.findUserChats(currentUserId, pageable);
        return page.map(chat -> mapToChatResponseDto(chat, currentUserId));
    }

    @Override
    public ChatDetailsResponseDto getChatDetails(UUID chatId, UUID currentUserId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ChatNotFoundException("Chat not found: " + chatId));

        chatMemberRepository.findByChatIdAndUserId(chatId, currentUserId)
                .orElseThrow(() -> new ChatAccessDeniedException("You are not a member of this chat"));

        String displayTitle;
        String displayAvatarUrl;
        String tourTitle = null;

        if (chat.getType() == ChatType.DIRECT) {
            UUID otherUserId = chat.getUser1().getId().equals(currentUserId)
                    ? chat.getUser2().getId()
                    : chat.getUser1().getId();
            UserProfile profile = userProfileRepository.findByUserId(otherUserId).orElse(null);
            displayTitle = profile != null && profile.getFullName() != null ? profile.getFullName() : "Direct Chat";
            displayAvatarUrl = profile != null ? profile.getAvatarUrl() : null;
        } else {
            tourTitle = chat.getTour() != null ? chat.getTour().getTitle() : null;
            displayTitle = chat.getTitle() != null ? chat.getTitle() : (tourTitle != null ? tourTitle : "Tour Group Chat");
            displayAvatarUrl = chat.getAvatarUrl() != null ? chat.getAvatarUrl() : (chat.getTour() != null ? chat.getTour().getCoverImageUrl() : null);
        }

        List<ChatMember> members = chatMemberRepository.findAllByChatId(chatId);
        List<ChatMemberResponseDto> memberDtos = members.stream()
                .map(m -> {
                    UserProfile p = userProfileRepository.findByUserId(m.getUser().getId()).orElse(null);
                    return chatMapper.toMemberResponseDto(m, p);
                })
                .toList();

        return chatMapper.toChatDetailsResponseDto(chat, displayTitle, displayAvatarUrl, tourTitle, memberDtos);
    }

    @Override
    @Transactional
    public ChatMessageResponseDto sendMessage(UUID chatId, UUID currentUserId, SendMessageRequestDto requestDto) {
        boolean hasContent = requestDto.content() != null && !requestDto.content().isBlank();
        boolean hasAttachment = requestDto.attachmentUrl() != null && !requestDto.attachmentUrl().isBlank();

        if (!hasContent && !hasAttachment) {
            throw new InvalidMessageContentException("Message must contain text content or an attachment URL");
        }

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ChatNotFoundException("Chat not found: " + chatId));

        ChatMember member = chatMemberRepository.findByChatIdAndUserId(chatId, currentUserId)
                .orElseThrow(() -> new ChatAccessDeniedException("You are not a member of this chat"));

        if (!member.isActive()) {
            throw new ChatAccessDeniedException("You are no longer an active member of this chat");
        }

        if (chat.getType() == ChatType.DIRECT) {
            UUID otherUserId = chat.getUser1().getId().equals(currentUserId)
                    ? chat.getUser2().getId()
                    : chat.getUser1().getId();
            if (userBlockRepository.isBlockedBetween(currentUserId, otherUserId)) {
                throw new UserBlockedException("Cannot send message: user is blocked");
            }
        }

        User sender = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUserId));

        MessageType messageType = requestDto.messageType() != null
                ? requestDto.messageType()
                : (hasAttachment && !hasContent ? MessageType.IMAGE : MessageType.TEXT);

        Message message = Message.builder()
                .chat(chat)
                .sender(sender)
                .messageType(messageType)
                .content(hasContent ? requestDto.content().trim() : null)
                .attachmentUrl(hasAttachment ? requestDto.attachmentUrl().trim() : null)
                .build();

        Message savedMessage = messageRepository.save(message);

        chat.setUpdatedAt(Instant.now());
        chatRepository.save(chat);

        member.setLastReadAt(Instant.now());
        chatMemberRepository.save(member);

        MessageRead senderRead = MessageRead.builder()
                .message(savedMessage)
                .user(sender)
                .chat(chat)
                .readAt(Instant.now())
                .build();
        messageReadRepository.save(senderRead);

        UserProfile senderProfile = userProfileRepository.findByUserId(currentUserId).orElse(null);
        ChatMessageResponseDto responseDto = chatMapper.toMessageResponseDto(savedMessage, senderProfile, true, 1, true);

        try {
            messagingTemplate.convertAndSend("/topic/chats/" + chatId, responseDto);
        } catch (Exception ex) {
            log.warn("Failed to broadcast chat message via STOMP: {}", ex.getMessage());
        }

        return responseDto;
    }

    @Override
    public Page<ChatMessageResponseDto> getChatMessages(UUID chatId, UUID currentUserId, Pageable pageable) {
        chatRepository.findById(chatId)
                .orElseThrow(() -> new ChatNotFoundException("Chat not found: " + chatId));

        ChatMember member = chatMemberRepository.findByChatIdAndUserId(chatId, currentUserId)
                .orElseThrow(() -> new ChatAccessDeniedException("You are not a member of this chat"));

        Page<Message> messages;
        if (member.getLeftAt() != null) {
            messages = messageRepository.findAllByChatIdAndCreatedAtLessThanEqualOrderByCreatedAtDesc(
                    chatId, member.getLeftAt(), pageable
            );
        } else {
            messages = messageRepository.findAllByChatIdOrderByCreatedAtDesc(chatId, pageable);
        }

        return messages.map(msg -> {
            UserProfile senderProfile = userProfileRepository.findByUserId(msg.getSender().getId()).orElse(null);
            boolean isMine = msg.getSender().getId().equals(currentUserId);
            int readCount = messageReadRepository.countByMessageId(msg.getId());
            boolean isRead = isMine || messageReadRepository.existsByMessageIdAndUserId(msg.getId(), currentUserId);
            return chatMapper.toMessageResponseDto(msg, senderProfile, isMine, readCount, isRead);
        });
    }

    @Override
    @Transactional
    public void markMessagesAsRead(UUID chatId, UUID currentUserId, MarkMessagesReadRequestDto requestDto) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ChatNotFoundException("Chat not found: " + chatId));

        ChatMember member = chatMemberRepository.findByChatIdAndUserId(chatId, currentUserId)
                .orElseThrow(() -> new ChatAccessDeniedException("You are not a member of this chat"));

        Instant upTo = null;
        if (requestDto != null && requestDto.lastMessageId() != null) {
            Message target = messageRepository.findById(requestDto.lastMessageId()).orElse(null);
            if (target != null && target.getChat().getId().equals(chatId)) {
                upTo = target.getCreatedAt();
            }
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUserId));

        List<Message> unread = (upTo != null)
                ? messageRepository.findUnreadMessagesUpTo(chatId, currentUserId, upTo)
                : messageRepository.findUnreadMessages(chatId, currentUserId);
        for (Message msg : unread) {
            MessageRead read = MessageRead.builder()
                    .message(msg)
                    .user(currentUser)
                    .chat(chat)
                    .readAt(Instant.now())
                    .build();
            messageReadRepository.save(read);
        }

        member.setLastReadAt(Instant.now());
        chatMemberRepository.save(member);
    }

    @Override
    @Transactional
    public void addTourParticipantToChat(UUID tourId, UUID participantId) {
        Tour tour = tourRepository.findById(tourId).orElse(null);
        if (tour == null) return;

        Chat chat = chatRepository.findByTourId(tourId).orElseGet(() -> {
            Chat newChat = chatRepository.save(Chat.createTourGroupChat(tour, tour.getOrganizer()));
            chatMemberRepository.save(ChatMember.builder().chat(newChat).user(tour.getOrganizer()).role(ChatMemberRole.ORGANIZER).build());
            return newChat;
        });

        User participant = userRepository.findById(participantId).orElse(null);
        if (participant == null) return;

        ChatMember member = chatMemberRepository.findByChatIdAndUserId(chat.getId(), participantId).orElse(null);
        if (member == null) {
            chatMemberRepository.save(ChatMember.builder().chat(chat).user(participant).role(ChatMemberRole.MEMBER).build());
        } else if (!member.isActive()) {
            member.setActive(true);
            member.setLeftAt(null);
            chatMemberRepository.save(member);
        }
        log.info("Participant {} added to tour {} group chat", participantId, tourId);
    }

    @Override
    @Transactional
    public void removeTourParticipantFromChat(UUID tourId, UUID participantId) {
        chatRepository.findByTourId(tourId).ifPresent(chat -> {
            chatMemberRepository.findByChatIdAndUserId(chat.getId(), participantId).ifPresent(member -> {
                member.setActive(false);
                member.setLeftAt(Instant.now());
                chatMemberRepository.save(member);
                log.info("Participant {} removed/deactivated from tour {} group chat", participantId, tourId);
            });
        });
    }

    @Override
    @Transactional
    public void deactivateChatForCancelledTour(UUID tourId) {
        chatRepository.findByTourId(tourId).ifPresent(chat -> {
            List<ChatMember> members = chatMemberRepository.findAllByChatIdAndIsActiveTrue(chat.getId());
            for (ChatMember m : members) {
                if (m.getRole() != ChatMemberRole.ORGANIZER) {
                    m.setActive(false);
                    m.setLeftAt(Instant.now());
                    chatMemberRepository.save(m);
                }
            }
            log.info("Tour group chat for tour {} deactivated for participants due to cancellation", tourId);
        });
    }

    private ChatResponseDto mapToChatResponseDto(Chat chat, UUID currentUserId) {
        String displayTitle;
        String displayAvatarUrl;
        String tourTitle = null;
        ChatRecipientDto recipient = null;

        if (chat.getType() == ChatType.DIRECT) {
            UUID otherUserId = chat.getUser1().getId().equals(currentUserId)
                    ? chat.getUser2().getId()
                    : chat.getUser1().getId();
            User otherUser = userRepository.findById(otherUserId).orElse(null);
            UserProfile profile = userProfileRepository.findByUserId(otherUserId).orElse(null);
            recipient = chatMapper.toRecipientDto(otherUser, profile);

            displayTitle = profile != null && profile.getFullName() != null
                    ? profile.getFullName()
                    : (profile != null && profile.getNickname() != null ? profile.getNickname() : "Direct Chat");
            displayAvatarUrl = profile != null ? profile.getAvatarUrl() : null;
        } else {
            tourTitle = chat.getTour() != null ? chat.getTour().getTitle() : null;
            displayTitle = chat.getTitle() != null ? chat.getTitle() : (tourTitle != null ? tourTitle : "Tour Group Chat");
            displayAvatarUrl = chat.getAvatarUrl() != null ? chat.getAvatarUrl() : (chat.getTour() != null ? chat.getTour().getCoverImageUrl() : null);
        }

        Message lastMsg = messageRepository.findFirstByChatIdOrderByCreatedAtDesc(chat.getId()).orElse(null);
        ChatMessageResponseDto lastMsgDto = null;
        if (lastMsg != null) {
            UserProfile senderProfile = userProfileRepository.findByUserId(lastMsg.getSender().getId()).orElse(null);
            boolean isMine = lastMsg.getSender().getId().equals(currentUserId);
            int readCount = messageReadRepository.countByMessageId(lastMsg.getId());
            boolean isRead = isMine || messageReadRepository.existsByMessageIdAndUserId(lastMsg.getId(), currentUserId);
            lastMsgDto = chatMapper.toMessageResponseDto(lastMsg, senderProfile, isMine, readCount, isRead);
        }

        ChatMember callerMember = chatMemberRepository.findByChatIdAndUserId(chat.getId(), currentUserId).orElse(null);
        int unreadCount;
        if (callerMember != null && callerMember.getLeftAt() != null) {
            unreadCount = (int) messageRepository.countUnreadMessagesBefore(chat.getId(), currentUserId, callerMember.getLeftAt());
        } else {
            unreadCount = (int) messageRepository.countUnreadMessages(chat.getId(), currentUserId);
        }

        int membersCount = (int) chatMemberRepository.countByChatIdAndIsActiveTrue(chat.getId());

        return chatMapper.toChatResponseDto(
                chat,
                displayTitle,
                displayAvatarUrl,
                tourTitle,
                lastMsgDto,
                unreadCount,
                membersCount,
                recipient
        );
    }
}
