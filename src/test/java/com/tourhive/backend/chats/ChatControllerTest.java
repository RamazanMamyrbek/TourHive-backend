package com.tourhive.backend.chats;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourhive.backend.configurations.security.JwtTokenProvider;
import com.tourhive.backend.dto.chats.request.CreateDirectChatRequestDto;
import com.tourhive.backend.dto.chats.request.SendMessageRequestDto;
import com.tourhive.backend.entities.chats.Chat;
import com.tourhive.backend.entities.chats.ChatMember;
import com.tourhive.backend.entities.chats.ChatMemberRole;
import com.tourhive.backend.entities.chats.ChatType;
import com.tourhive.backend.entities.chats.MessageType;
import com.tourhive.backend.entities.friendships.Friendship;
import com.tourhive.backend.entities.tours.Tour;
import com.tourhive.backend.entities.tours.TourStatus;
import com.tourhive.backend.entities.tours.TourType;
import com.tourhive.backend.entities.users.Role;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserProfile;
import com.tourhive.backend.entities.users.UserStatus;
import com.tourhive.backend.repositories.chats.ChatMemberRepository;
import com.tourhive.backend.repositories.chats.ChatRepository;
import com.tourhive.backend.repositories.chats.MessageReadRepository;
import com.tourhive.backend.repositories.chats.MessageRepository;
import com.tourhive.backend.repositories.friendships.FriendshipRepository;
import com.tourhive.backend.repositories.tours.TourRepository;
import com.tourhive.backend.repositories.users.UserBlockRepository;
import com.tourhive.backend.repositories.users.UserProfileRepository;
import com.tourhive.backend.repositories.users.UserRepository;
import com.tourhive.backend.services.users.UserBlockService;
import com.tourhive.backend.support.DatabaseCleanup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private TourRepository tourRepository;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private ChatMemberRepository chatMemberRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private MessageReadRepository messageReadRepository;

    @Autowired
    private UserBlockService userBlockService;

    @Autowired
    private DatabaseCleanup databaseCleanup;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User alice;
    private User bob;
    private User charlie;
    private String tokenAlice;
    private String tokenBob;
    private String tokenCharlie;
    private Tour tour;

    @BeforeEach
    void setUp() {
        databaseCleanup.clean();

        alice = userRepository.save(User.builder()
                .email("alice@tourhive.com")
                .passwordHash("hash")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build());

        bob = userRepository.save(User.builder()
                .email("bob@tourhive.com")
                .passwordHash("hash")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build());

        charlie = userRepository.save(User.builder()
                .email("charlie@tourhive.com")
                .passwordHash("hash")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build());

        userProfileRepository.save(UserProfile.builder().user(alice).firstName("Алиса").lastName("Смирнова").nickname("alice").build());
        userProfileRepository.save(UserProfile.builder().user(bob).firstName("Боб").lastName("Иванов").nickname("bob").build());
        userProfileRepository.save(UserProfile.builder().user(charlie).firstName("Чарли").lastName("Браун").nickname("charlie").build());

        tokenAlice = jwtTokenProvider.generateAccessToken(alice);
        tokenBob = jwtTokenProvider.generateAccessToken(bob);
        tokenCharlie = jwtTokenProvider.generateAccessToken(charlie);

        tour = tourRepository.save(Tour.builder()
                .organizer(bob)
                .title("Поход в горы")
                .description("Красивый маршрут")
                .tourType(TourType.COMMUNITY)
                .status(TourStatus.PUBLISHED)
                .capacity(10)
                .availablePlaces(10)
                .startDate(LocalDate.now().plusDays(5))
                .endDate(LocalDate.now().plusDays(7))
                .price(BigDecimal.valueOf(15000))
                .currency("KZT")
                .build());
    }

    @Test
    @DisplayName("POST /api/v1/chats/direct without token should return 401")
    void getOrCreateDirectChatUnauthorized() throws Exception {
        CreateDirectChatRequestDto request = new CreateDirectChatRequestDto(bob.getId());

        mockMvc.perform(post("/api/v1/chats/direct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/chats/direct with self should return 400 CANNOT_CHAT_WITH_SELF")
    void getOrCreateDirectChatWithSelf() throws Exception {
        CreateDirectChatRequestDto request = new CreateDirectChatRequestDto(alice.getId());

        mockMvc.perform(post("/api/v1/chats/direct")
                        .header("Authorization", "Bearer " + tokenAlice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CANNOT_CHAT_WITH_SELF"));
    }

    @Test
    @DisplayName("POST /api/v1/chats/direct between strangers should return 403 CHAT_ACCESS_DENIED")
    void getOrCreateDirectChatBetweenStrangers() throws Exception {
        CreateDirectChatRequestDto request = new CreateDirectChatRequestDto(charlie.getId());

        mockMvc.perform(post("/api/v1/chats/direct")
                        .header("Authorization", "Bearer " + tokenAlice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CHAT_ACCESS_DENIED"));
    }

    @Test
    @DisplayName("POST /api/v1/chats/direct when blocked should return 403 USER_BLOCKED")
    void getOrCreateDirectChatWhenBlocked() throws Exception {
        friendshipRepository.save(Friendship.builder().user1(alice).user2(bob).build());
        userBlockService.blockUser(alice.getId(), bob.getId());

        CreateDirectChatRequestDto request = new CreateDirectChatRequestDto(bob.getId());

        mockMvc.perform(post("/api/v1/chats/direct")
                        .header("Authorization", "Bearer " + tokenAlice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_BLOCKED"));
    }

    @Test
    @DisplayName("POST /api/v1/chats/direct between friends should return 200 OK and create chat")
    void getOrCreateDirectChatBetweenFriends() throws Exception {
        friendshipRepository.save(Friendship.builder().user1(alice).user2(bob).build());

        CreateDirectChatRequestDto request = new CreateDirectChatRequestDto(bob.getId());

        mockMvc.perform(post("/api/v1/chats/direct")
                        .header("Authorization", "Bearer " + tokenAlice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("DIRECT"))
                .andExpect(jsonPath("$.data.title").value("Боб Иванов"))
                .andExpect(jsonPath("$.data.membersCount").value(2));
    }

    @Test
    @DisplayName("GET /api/v1/chats/tours/{tourId} by organizer should return 200 OK")
    void getOrCreateTourGroupChatByOrganizer() throws Exception {
        mockMvc.perform(get("/api/v1/chats/tours/" + tour.getId())
                        .header("Authorization", "Bearer " + tokenBob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("TOUR_GROUP"))
                .andExpect(jsonPath("$.data.tourTitle").value("Поход в горы"));
    }

    @Test
    @DisplayName("GET /api/v1/chats/tours/{tourId} by stranger should return 403 CHAT_ACCESS_DENIED")
    void getOrCreateTourGroupChatByStranger() throws Exception {
        mockMvc.perform(get("/api/v1/chats/tours/" + tour.getId())
                        .header("Authorization", "Bearer " + tokenCharlie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CHAT_ACCESS_DENIED"));
    }

    @Test
    @DisplayName("POST /api/v1/chats/{id}/messages should send message and update chat")
    void sendMessageSucceeds() throws Exception {
        friendshipRepository.save(Friendship.builder().user1(alice).user2(bob).build());

        Chat chat = chatRepository.save(Chat.createDirectChat(alice, bob, alice));
        chatMemberRepository.save(ChatMember.builder().chat(chat).user(alice).role(ChatMemberRole.MEMBER).build());
        chatMemberRepository.save(ChatMember.builder().chat(chat).user(bob).role(ChatMemberRole.MEMBER).build());

        SendMessageRequestDto messageRequest = new SendMessageRequestDto("Привет, Боб!", MessageType.TEXT, null);

        mockMvc.perform(post("/api/v1/chats/" + chat.getId() + "/messages")
                        .header("Authorization", "Bearer " + tokenAlice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(messageRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.content").value("Привет, Боб!"))
                .andExpect(jsonPath("$.data.isMine").value(true));

        // Bob checks unread count in his chats
        mockMvc.perform(get("/api/v1/chats")
                        .header("Authorization", "Bearer " + tokenBob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].unreadCount").value(1))
                .andExpect(jsonPath("$.data.content[0].lastMessage.content").value("Привет, Боб!"));
    }

    @Test
    @DisplayName("POST /api/v1/chats/{id}/read should mark messages as read")
    void markMessagesAsRead() throws Exception {
        friendshipRepository.save(Friendship.builder().user1(alice).user2(bob).build());

        Chat chat = chatRepository.save(Chat.createDirectChat(alice, bob, alice));
        chatMemberRepository.save(ChatMember.builder().chat(chat).user(alice).role(ChatMemberRole.MEMBER).build());
        chatMemberRepository.save(ChatMember.builder().chat(chat).user(bob).role(ChatMemberRole.MEMBER).build());

        SendMessageRequestDto messageRequest = new SendMessageRequestDto("Привет!", MessageType.TEXT, null);

        mockMvc.perform(post("/api/v1/chats/" + chat.getId() + "/messages")
                        .header("Authorization", "Bearer " + tokenAlice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(messageRequest)))
                .andExpect(status().isCreated());

        // Bob marks as read
        mockMvc.perform(post("/api/v1/chats/" + chat.getId() + "/read")
                        .header("Authorization", "Bearer " + tokenBob))
                .andExpect(status().isOk());

        // Bob checks unread count is now 0
        mockMvc.perform(get("/api/v1/chats")
                        .header("Authorization", "Bearer " + tokenBob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].unreadCount").value(0));
    }

    @Test
    @DisplayName("POST /api/v1/chats/{id}/messages fails with 400 when content is blank")
    void sendMessageFailsWhenEmpty() throws Exception {
        Chat chat = chatRepository.save(Chat.createDirectChat(alice, bob, alice));
        chatMemberRepository.save(ChatMember.builder().chat(chat).user(alice).role(ChatMemberRole.MEMBER).build());

        SendMessageRequestDto emptyRequest = new SendMessageRequestDto("   ", MessageType.TEXT, null);

        mockMvc.perform(post("/api/v1/chats/" + chat.getId() + "/messages")
                        .header("Authorization", "Bearer " + tokenAlice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_MESSAGE_CONTENT"));
    }
}
