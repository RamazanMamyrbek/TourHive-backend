package com.tourhive.backend.chats;

import com.tourhive.backend.dto.chats.request.CreateDirectChatRequestDto;
import com.tourhive.backend.dto.chats.request.MarkMessagesReadRequestDto;
import com.tourhive.backend.dto.chats.request.SendMessageRequestDto;
import com.tourhive.backend.dto.chats.response.ChatDetailsResponseDto;
import com.tourhive.backend.dto.chats.response.ChatMessageResponseDto;
import com.tourhive.backend.dto.chats.response.ChatResponseDto;
import com.tourhive.backend.entities.chats.Chat;
import com.tourhive.backend.entities.chats.ChatMember;
import com.tourhive.backend.entities.chats.ChatMemberRole;
import com.tourhive.backend.entities.chats.ChatType;
import com.tourhive.backend.entities.chats.Message;
import com.tourhive.backend.entities.chats.MessageType;
import com.tourhive.backend.entities.tours.Tour;
import com.tourhive.backend.entities.tours.TourApplicationStatus;
import com.tourhive.backend.entities.users.Role;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserProfile;
import com.tourhive.backend.exceptions.CannotChatWithSelfException;
import com.tourhive.backend.exceptions.ChatAccessDeniedException;
import com.tourhive.backend.exceptions.InvalidMessageContentException;
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
import com.tourhive.backend.services.chats.impl.ChatServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private ChatMemberRepository chatMemberRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private MessageReadRepository messageReadRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private TourRepository tourRepository;

    @Mock
    private TourApplicationRepository tourApplicationRepository;

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private UserBlockRepository userBlockRepository;

    @Spy
    private ChatMapper chatMapper = new ChatMapper();

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ChatServiceImpl chatService;

    private User alice;
    private User bob;
    private User charlie;
    private UserProfile aliceProfile;
    private UserProfile bobProfile;
    private Tour tour;

    @BeforeEach
    void setUp() {
        alice = User.builder().email("alice@test.com").role(Role.USER).build();
        alice.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));

        bob = User.builder().email("bob@test.com").role(Role.USER).build();
        bob.setId(UUID.fromString("22222222-2222-2222-2222-222222222222"));

        charlie = User.builder().email("charlie@test.com").role(Role.USER).build();
        charlie.setId(UUID.fromString("33333333-3333-3333-3333-333333333333"));

        aliceProfile = UserProfile.builder().user(alice).firstName("Алиса").lastName("Смирнова").nickname("alice").build();
        bobProfile = UserProfile.builder().user(bob).firstName("Боб").lastName("Иванов").nickname("bob").build();

        tour = Tour.builder().title("Алматинский поход").organizer(bob).build();
        tour.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Cannot chat with self should throw CannotChatWithSelfException")
    void cannotChatWithSelf() {
        CreateDirectChatRequestDto request = new CreateDirectChatRequestDto(alice.getId());

        assertThatThrownBy(() -> chatService.getOrCreateDirectChat(alice.getId(), request))
                .isInstanceOf(CannotChatWithSelfException.class);
    }

    @Test
    @DisplayName("Cannot chat if users are blocked should throw UserBlockedException")
    void cannotChatIfBlocked() {
        when(userRepository.findById(alice.getId())).thenReturn(Optional.of(alice));
        when(userRepository.findById(bob.getId())).thenReturn(Optional.of(bob));
        when(userBlockRepository.isBlockedBetween(alice.getId(), bob.getId())).thenReturn(true);

        CreateDirectChatRequestDto request = new CreateDirectChatRequestDto(bob.getId());

        assertThatThrownBy(() -> chatService.getOrCreateDirectChat(alice.getId(), request))
                .isInstanceOf(UserBlockedException.class);
    }

    @Test
    @DisplayName("Cannot direct chat if neither friends nor tour-related should throw ChatAccessDeniedException")
    void cannotChatIfNotFriendsOrTourRelated() {
        when(userRepository.findById(alice.getId())).thenReturn(Optional.of(alice));
        when(userRepository.findById(charlie.getId())).thenReturn(Optional.of(charlie));
        when(userBlockRepository.isBlockedBetween(alice.getId(), charlie.getId())).thenReturn(false);
        when(friendshipRepository.areFriends(alice.getId(), charlie.getId())).thenReturn(false);
        when(tourApplicationRepository.hasTourRelationshipBetween(alice.getId(), charlie.getId())).thenReturn(false);

        CreateDirectChatRequestDto request = new CreateDirectChatRequestDto(charlie.getId());

        assertThatThrownBy(() -> chatService.getOrCreateDirectChat(alice.getId(), request))
                .isInstanceOf(ChatAccessDeniedException.class)
                .hasMessageContaining("Direct chat is only available between friends or tour participants and organizer");
    }

    @Test
    @DisplayName("Successfully create direct chat between friends")
    void createDirectChatBetweenFriends() {
        when(userRepository.findById(alice.getId())).thenReturn(Optional.of(alice));
        when(userRepository.findById(bob.getId())).thenReturn(Optional.of(bob));
        when(userBlockRepository.isBlockedBetween(alice.getId(), bob.getId())).thenReturn(false);
        when(friendshipRepository.areFriends(alice.getId(), bob.getId())).thenReturn(true);
        when(chatRepository.findDirectChatBetween(alice.getId(), bob.getId())).thenReturn(Optional.empty());

        Chat createdChat = Chat.createDirectChat(alice, bob, alice);
        createdChat.setId(UUID.randomUUID());
        createdChat.setUpdatedAt(Instant.now());
        when(chatRepository.save(any(Chat.class))).thenReturn(createdChat);
        when(userProfileRepository.findByUserId(bob.getId())).thenReturn(Optional.of(bobProfile));
        when(messageRepository.findFirstByChatIdOrderByCreatedAtDesc(createdChat.getId())).thenReturn(Optional.empty());
        when(chatMemberRepository.countByChatIdAndIsActiveTrue(createdChat.getId())).thenReturn(2L);

        ChatResponseDto result = chatService.getOrCreateDirectChat(alice.getId(), new CreateDirectChatRequestDto(bob.getId()));

        assertThat(result).isNotNull();
        assertThat(result.type()).isEqualTo(ChatType.DIRECT);
        assertThat(result.title()).isEqualTo("Боб Иванов");
        assertThat(result.membersCount()).isEqualTo(2);
        verify(chatMemberRepository, times(2)).save(any(ChatMember.class));
    }

    @Test
    @DisplayName("Tour group chat access denied for non-participant stranger")
    void tourGroupChatAccessDeniedForStranger() {
        when(tourRepository.findById(tour.getId())).thenReturn(Optional.of(tour));
        when(userRepository.findById(charlie.getId())).thenReturn(Optional.of(charlie));
        when(tourApplicationRepository.existsByTourIdAndApplicantIdAndStatus(tour.getId(), charlie.getId(), TourApplicationStatus.ACCEPTED))
                .thenReturn(false);

        assertThatThrownBy(() -> chatService.getOrCreateTourGroupChat(tour.getId(), charlie.getId()))
                .isInstanceOf(ChatAccessDeniedException.class)
                .hasMessageContaining("Only tour organizer and accepted participants can access the tour group chat");
    }

    @Test
    @DisplayName("Tour group chat accessible by tour organizer")
    void tourGroupChatAccessibleByOrganizer() {
        when(tourRepository.findById(tour.getId())).thenReturn(Optional.of(tour));
        when(userRepository.findById(bob.getId())).thenReturn(Optional.of(bob));

        Chat groupChat = Chat.createTourGroupChat(tour, bob);
        groupChat.setId(UUID.randomUUID());
        groupChat.setUpdatedAt(Instant.now());

        when(chatRepository.findByTourId(tour.getId())).thenReturn(Optional.empty());
        when(chatRepository.save(any(Chat.class))).thenReturn(groupChat);
        when(chatMemberRepository.findByChatIdAndUserId(groupChat.getId(), bob.getId())).thenReturn(Optional.empty());
        when(messageRepository.findFirstByChatIdOrderByCreatedAtDesc(groupChat.getId())).thenReturn(Optional.empty());
        when(chatMemberRepository.countByChatIdAndIsActiveTrue(groupChat.getId())).thenReturn(1L);

        ChatResponseDto result = chatService.getOrCreateTourGroupChat(tour.getId(), bob.getId());

        assertThat(result).isNotNull();
        assertThat(result.type()).isEqualTo(ChatType.TOUR_GROUP);
        assertThat(result.title()).isEqualTo("Алматинский поход");
    }

    @Test
    @DisplayName("Send message fails when content and attachment are both blank")
    void sendMessageFailsWhenEmpty() {
        UUID chatId = UUID.randomUUID();
        SendMessageRequestDto request = new SendMessageRequestDto("   ", MessageType.TEXT, null);

        assertThatThrownBy(() -> chatService.sendMessage(chatId, alice.getId(), request))
                .isInstanceOf(InvalidMessageContentException.class);
    }

    @Test
    @DisplayName("Send message succeeds and broadcasts via WebSocket")
    void sendMessageSucceeds() {
        UUID chatId = UUID.randomUUID();
        Chat chat = Chat.createDirectChat(alice, bob, alice);
        chat.setId(chatId);

        ChatMember member = ChatMember.builder().chat(chat).user(alice).isActive(true).build();

        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(chatMemberRepository.findByChatIdAndUserId(chatId, alice.getId())).thenReturn(Optional.of(member));
        when(userBlockRepository.isBlockedBetween(alice.getId(), bob.getId())).thenReturn(false);
        when(userRepository.findById(alice.getId())).thenReturn(Optional.of(alice));
        when(userProfileRepository.findByUserId(alice.getId())).thenReturn(Optional.of(aliceProfile));

        Message savedMsg = Message.builder()
                .chat(chat)
                .sender(alice)
                .content("Привет, Боб!")
                .messageType(MessageType.TEXT)
                .build();
        savedMsg.setId(UUID.randomUUID());
        savedMsg.setCreatedAt(Instant.now());

        when(messageRepository.save(any(Message.class))).thenReturn(savedMsg);

        SendMessageRequestDto request = new SendMessageRequestDto("Привет, Боб!", MessageType.TEXT, null);
        ChatMessageResponseDto result = chatService.sendMessage(chatId, alice.getId(), request);

        assertThat(result).isNotNull();
        assertThat(result.content()).isEqualTo("Привет, Боб!");
        assertThat(result.isMine()).isTrue();
        verify(messagingTemplate).convertAndSend(eq("/topic/chats/" + chatId), any(ChatMessageResponseDto.class));
        verify(messageReadRepository).save(any());
    }

    @Test
    @DisplayName("Send message fails when user has left the chat")
    void sendMessageFailsWhenLeft() {
        UUID chatId = UUID.randomUUID();
        Chat chat = Chat.createDirectChat(alice, bob, alice);
        chat.setId(chatId);

        ChatMember member = ChatMember.builder().chat(chat).user(alice).isActive(false).leftAt(Instant.now()).build();

        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(chatMemberRepository.findByChatIdAndUserId(chatId, alice.getId())).thenReturn(Optional.of(member));

        SendMessageRequestDto request = new SendMessageRequestDto("Сообщение", MessageType.TEXT, null);

        assertThatThrownBy(() -> chatService.sendMessage(chatId, alice.getId(), request))
                .isInstanceOf(ChatAccessDeniedException.class)
                .hasMessageContaining("no longer an active member");
    }

    @Test
    @DisplayName("Get chat messages respects leftAt timestamp for former participants")
    void getChatMessagesRespectsLeftAt() {
        UUID chatId = UUID.randomUUID();
        Chat chat = Chat.createTourGroupChat(tour, bob);
        chat.setId(chatId);

        Instant leftTime = Instant.now().minusSeconds(3600);
        ChatMember member = ChatMember.builder().chat(chat).user(alice).isActive(false).leftAt(leftTime).build();

        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(chatMemberRepository.findByChatIdAndUserId(chatId, alice.getId())).thenReturn(Optional.of(member));

        PageRequest pageable = PageRequest.of(0, 20);
        when(messageRepository.findAllByChatIdAndCreatedAtLessThanEqualOrderByCreatedAtDesc(chatId, leftTime, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        Page<ChatMessageResponseDto> result = chatService.getChatMessages(chatId, alice.getId(), pageable);

        assertThat(result).isNotNull();
        verify(messageRepository).findAllByChatIdAndCreatedAtLessThanEqualOrderByCreatedAtDesc(chatId, leftTime, pageable);
        verify(messageRepository, never()).findAllByChatIdOrderByCreatedAtDesc(any(), any());
    }

    @Test
    @DisplayName("Mark messages as read updates member lastReadAt and saves message reads")
    void markMessagesAsRead() {
        UUID chatId = UUID.randomUUID();
        Chat chat = Chat.createDirectChat(alice, bob, alice);
        chat.setId(chatId);

        ChatMember member = ChatMember.builder().chat(chat).user(alice).isActive(true).build();
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(chatMemberRepository.findByChatIdAndUserId(chatId, alice.getId())).thenReturn(Optional.of(member));
        when(userRepository.findById(alice.getId())).thenReturn(Optional.of(alice));

        Message msg1 = Message.builder().chat(chat).sender(bob).content("Hi").build();
        when(messageRepository.findUnreadMessages(chatId, alice.getId())).thenReturn(List.of(msg1));

        chatService.markMessagesAsRead(chatId, alice.getId(), new MarkMessagesReadRequestDto(null));

        verify(messageReadRepository, times(1)).save(any());
        verify(chatMemberRepository).save(member);
    }

    @Test
    @DisplayName("Mark messages as read up to specific message ID")
    void markMessagesAsReadUpTo() {
        UUID chatId = UUID.randomUUID();
        Chat chat = Chat.createDirectChat(alice, bob, alice);
        chat.setId(chatId);

        ChatMember member = ChatMember.builder().chat(chat).user(alice).isActive(true).build();
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(chatMemberRepository.findByChatIdAndUserId(chatId, alice.getId())).thenReturn(Optional.of(member));
        when(userRepository.findById(alice.getId())).thenReturn(Optional.of(alice));

        UUID msgId = UUID.randomUUID();
        Message targetMsg = Message.builder().chat(chat).sender(bob).content("Hi").build();
        targetMsg.setId(msgId);
        targetMsg.setCreatedAt(Instant.now());

        when(messageRepository.findById(msgId)).thenReturn(Optional.of(targetMsg));
        when(messageRepository.findUnreadMessagesUpTo(chatId, alice.getId(), targetMsg.getCreatedAt()))
                .thenReturn(List.of(targetMsg));

        chatService.markMessagesAsRead(chatId, alice.getId(), new MarkMessagesReadRequestDto(msgId));

        verify(messageReadRepository, times(1)).save(any());
        verify(chatMemberRepository).save(member);
    }
}
