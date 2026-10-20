package com.tourhive.backend.friendships;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourhive.backend.configurations.security.JwtTokenProvider;
import com.tourhive.backend.dto.friendships.request.SendFriendRequestDto;
import com.tourhive.backend.entities.friendships.FriendRequest;
import com.tourhive.backend.entities.friendships.FriendRequestStatus;
import com.tourhive.backend.entities.friendships.Friendship;
import com.tourhive.backend.entities.users.Role;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserProfile;
import com.tourhive.backend.entities.users.UserStatus;
import com.tourhive.backend.repositories.friendships.FriendRequestRepository;
import com.tourhive.backend.repositories.friendships.FriendshipRepository;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FriendshipControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private FriendRequestRepository friendRequestRepository;

    @Autowired
    private FriendshipRepository friendshipRepository;

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

        userProfileRepository.save(UserProfile.builder().user(alice).nickname("alice").firstName("Alice").lastName("Smith").build());
        userProfileRepository.save(UserProfile.builder().user(bob).nickname("bob").firstName("Bob").lastName("Jones").build());
        userProfileRepository.save(UserProfile.builder().user(charlie).nickname("charlie").firstName("Charlie").lastName("Brown").build());

        tokenAlice = "Bearer " + jwtTokenProvider.generateAccessToken(alice);
        tokenBob = "Bearer " + jwtTokenProvider.generateAccessToken(bob);
        tokenCharlie = "Bearer " + jwtTokenProvider.generateAccessToken(charlie);
    }

    @Test
    @DisplayName("Should send friend request successfully")
    void shouldSendFriendRequestSuccessfully() throws Exception {
        SendFriendRequestDto dto = new SendFriendRequestDto(bob.getId(), "Hi Bob, let's explore together!");

        mockMvc.perform(post("/api/v1/friends/requests")
                        .header("Authorization", tokenAlice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.senderId", is(alice.getId().toString())))
                .andExpect(jsonPath("$.data.receiverId", is(bob.getId().toString())))
                .andExpect(jsonPath("$.data.status", is("PENDING")))
                .andExpect(jsonPath("$.data.message", is("Hi Bob, let's explore together!")));

        assertThat(friendRequestRepository.existsBySenderIdAndReceiverIdAndStatus(alice.getId(), bob.getId(), FriendRequestStatus.PENDING)).isTrue();
    }

    @Test
    @DisplayName("Should reject self friend request with 400 CANNOT_BEFRIEND_SELF")
    void shouldRejectSelfFriendRequest() throws Exception {
        SendFriendRequestDto dto = new SendFriendRequestDto(alice.getId(), "Hi myself");

        mockMvc.perform(post("/api/v1/friends/requests")
                        .header("Authorization", tokenAlice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("CANNOT_BEFRIEND_SELF")));
    }

    @Test
    @DisplayName("Should reject duplicate pending request with 409 DUPLICATE_FRIEND_REQUEST")
    void shouldRejectDuplicatePendingFriendRequest() throws Exception {
        friendRequestRepository.save(FriendRequest.builder()
                .sender(alice)
                .receiver(bob)
                .status(FriendRequestStatus.PENDING)
                .message("Request 1")
                .build());

        SendFriendRequestDto dto = new SendFriendRequestDto(bob.getId(), "Request 2");

        mockMvc.perform(post("/api/v1/friends/requests")
                        .header("Authorization", tokenAlice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("DUPLICATE_FRIEND_REQUEST")));
    }

    @Test
    @DisplayName("Should reject friend request when users are blocked with 403 USER_BLOCKED")
    void shouldRejectFriendRequestWhenBlocked() throws Exception {
        userBlockService.blockUser(bob.getId(), alice.getId());

        SendFriendRequestDto dto = new SendFriendRequestDto(bob.getId(), "Can we be friends?");

        mockMvc.perform(post("/api/v1/friends/requests")
                        .header("Authorization", tokenAlice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("USER_BLOCKED")));
    }

    @Test
    @DisplayName("Should get incoming and outgoing requests")
    void shouldGetIncomingAndOutgoingRequests() throws Exception {
        friendRequestRepository.save(FriendRequest.builder()
                .sender(alice)
                .receiver(bob)
                .status(FriendRequestStatus.PENDING)
                .message("Incoming for Bob")
                .build());

        mockMvc.perform(get("/api/v1/friends/requests/incoming")
                        .header("Authorization", tokenBob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].senderNickname", is("alice")));

        mockMvc.perform(get("/api/v1/friends/requests/outgoing")
                        .header("Authorization", tokenAlice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].receiverNickname", is("bob")));
    }

    @Test
    @DisplayName("Should accept friend request and create mutual friendship")
    void shouldAcceptFriendRequestAndCreateFriendship() throws Exception {
        FriendRequest request = friendRequestRepository.save(FriendRequest.builder()
                .sender(alice)
                .receiver(bob)
                .status(FriendRequestStatus.PENDING)
                .build());

        mockMvc.perform(post("/api/v1/friends/requests/" + request.getId() + "/accept")
                        .header("Authorization", tokenBob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("ACCEPTED")));

        assertThat(friendshipRepository.areFriends(alice.getId(), bob.getId())).isTrue();
    }

    @Test
    @DisplayName("Should reject accept when caller is not the receiver with 403")
    void shouldRejectAcceptWhenCallerIsNotReceiver() throws Exception {
        FriendRequest request = friendRequestRepository.save(FriendRequest.builder()
                .sender(alice)
                .receiver(bob)
                .status(FriendRequestStatus.PENDING)
                .build());

        mockMvc.perform(post("/api/v1/friends/requests/" + request.getId() + "/accept")
                        .header("Authorization", tokenCharlie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("FRIEND_REQUEST_ACCESS_DENIED")));
    }

    @Test
    @DisplayName("Should decline friend request")
    void shouldDeclineFriendRequest() throws Exception {
        FriendRequest request = friendRequestRepository.save(FriendRequest.builder()
                .sender(alice)
                .receiver(bob)
                .status(FriendRequestStatus.PENDING)
                .build());

        mockMvc.perform(post("/api/v1/friends/requests/" + request.getId() + "/decline")
                        .header("Authorization", tokenBob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("DECLINED")));

        assertThat(friendshipRepository.areFriends(alice.getId(), bob.getId())).isFalse();
    }

    @Test
    @DisplayName("Should cancel friend request by sender")
    void shouldCancelFriendRequestBySender() throws Exception {
        FriendRequest request = friendRequestRepository.save(FriendRequest.builder()
                .sender(alice)
                .receiver(bob)
                .status(FriendRequestStatus.PENDING)
                .build());

        mockMvc.perform(post("/api/v1/friends/requests/" + request.getId() + "/cancel")
                        .header("Authorization", tokenAlice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("CANCELLED")));

        assertThat(friendshipRepository.areFriends(alice.getId(), bob.getId())).isFalse();
    }

    @Test
    @DisplayName("Should list mutual friends and check status")
    void shouldListFriendsAndCheckStatus() throws Exception {
        // Create friendship
        User u1 = alice.getId().compareTo(bob.getId()) < 0 ? alice : bob;
        User u2 = alice.getId().compareTo(bob.getId()) < 0 ? bob : alice;
        friendshipRepository.save(new Friendship(u1, u2));

        mockMvc.perform(get("/api/v1/friends")
                        .header("Authorization", tokenAlice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].userId", is(bob.getId().toString())))
                .andExpect(jsonPath("$.data.content[0].nickname", is("bob")));

        mockMvc.perform(get("/api/v1/friends/" + bob.getId() + "/status")
                        .header("Authorization", tokenAlice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("FRIENDS")));
    }

    @Test
    @DisplayName("Should unfriend user")
    void shouldUnfriendUser() throws Exception {
        User u1 = alice.getId().compareTo(bob.getId()) < 0 ? alice : bob;
        User u2 = alice.getId().compareTo(bob.getId()) < 0 ? bob : alice;
        friendshipRepository.save(new Friendship(u1, u2));

        mockMvc.perform(delete("/api/v1/friends/" + bob.getId())
                        .header("Authorization", tokenAlice))
                .andExpect(status().isNoContent());

        assertThat(friendshipRepository.areFriends(alice.getId(), bob.getId())).isFalse();
    }
}
