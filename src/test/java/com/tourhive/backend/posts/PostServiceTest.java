package com.tourhive.backend.posts;

import com.tourhive.backend.dto.posts.request.CreatePostRequestDto;
import com.tourhive.backend.dto.posts.response.PostResponseDto;
import com.tourhive.backend.entities.posts.Post;
import com.tourhive.backend.entities.posts.PostLike;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserProfile;
import com.tourhive.backend.repositories.posts.PostLikeRepository;
import com.tourhive.backend.repositories.posts.PostRepository;
import com.tourhive.backend.repositories.users.UserProfileRepository;
import com.tourhive.backend.repositories.users.UserRepository;
import com.tourhive.backend.services.posts.impl.PostServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @InjectMocks
    private PostServiceImpl postService;

    private User author;
    private User otherUser;
    private UserProfile profile;
    private Post samplePost;

    @BeforeEach
    void setUp() {
        author = User.builder().email("author@example.com").build();
        author.setId(UUID.randomUUID());

        otherUser = User.builder().email("other@example.com").build();
        otherUser.setId(UUID.randomUUID());

        profile = UserProfile.builder()
                .user(author)
                .firstName("John")
                .lastName("Doe")
                .nickname("johndoe")
                .avatarUrl("http://example.com/avatar.jpg")
                .build();

        samplePost = Post.builder()
                .author(author)
                .imageUrl("http://example.com/photo.jpg")
                .caption("Awesome trip!")
                .location("Rome, Italy")
                .likesCount(0)
                .build();
        samplePost.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("createPost creates post and returns DTO with author info")
    void createPostSuccess() {
        CreatePostRequestDto req = new CreatePostRequestDto("http://example.com/photo.jpg", "Awesome trip!", "Rome, Italy");

        when(userRepository.findById(author.getId())).thenReturn(Optional.of(author));
        when(postRepository.save(any(Post.class))).thenReturn(samplePost);
        when(userProfileRepository.findByUserId(author.getId())).thenReturn(Optional.of(profile));

        PostResponseDto res = postService.createPost(author.getId(), req);

        assertThat(res).isNotNull();
        assertThat(res.authorName()).isEqualTo("John Doe");
        assertThat(res.authorNickname()).isEqualTo("johndoe");
        assertThat(res.caption()).isEqualTo("Awesome trip!");
        assertThat(res.location()).isEqualTo("Rome, Italy");
        verify(postRepository).save(any(Post.class));
    }

    @Test
    @DisplayName("getUserPosts returns paged posts for author")
    void getUserPostsSuccess() {
        when(postRepository.findAllByAuthorIdOrderByCreatedAtDesc(eq(author.getId()), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(samplePost)));
        when(userProfileRepository.findByUserId(author.getId())).thenReturn(Optional.of(profile));

        Page<PostResponseDto> page = postService.getUserPosts(author.getId(), author.getId(), PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).authorNickname()).isEqualTo("johndoe");
    }

    @Test
    @DisplayName("toggleLike likes post when not liked before, unlikes when already liked")
    void toggleLikeBehavior() {
        when(postRepository.findById(samplePost.getId())).thenReturn(Optional.of(samplePost));
        when(userRepository.findById(author.getId())).thenReturn(Optional.of(author));
        when(postLikeRepository.findByPostIdAndUserId(samplePost.getId(), author.getId())).thenReturn(Optional.empty());
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userProfileRepository.findByUserId(author.getId())).thenReturn(Optional.of(profile));

        // Like
        PostResponseDto liked = postService.toggleLike(author.getId(), samplePost.getId());
        assertThat(liked.likesCount()).isEqualTo(1);
        verify(postLikeRepository).save(any(PostLike.class));

        // Unlike
        PostLike existingLike = PostLike.builder().post(samplePost).user(author).build();
        when(postLikeRepository.findByPostIdAndUserId(samplePost.getId(), author.getId())).thenReturn(Optional.of(existingLike));
        PostResponseDto unliked = postService.toggleLike(author.getId(), samplePost.getId());
        assertThat(unliked.likesCount()).isEqualTo(0);
        verify(postLikeRepository).delete(existingLike);
    }

    @Test
    @DisplayName("deletePost throws AccessDeniedException when non-author attempts delete")
    void deletePostDeniedForOtherUser() {
        when(postRepository.findById(samplePost.getId())).thenReturn(Optional.of(samplePost));

        assertThatThrownBy(() -> postService.deletePost(otherUser.getId(), samplePost.getId()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not the author");

        verify(postRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deletePost deletes post when called by author")
    void deletePostSuccessForAuthor() {
        when(postRepository.findById(samplePost.getId())).thenReturn(Optional.of(samplePost));

        postService.deletePost(author.getId(), samplePost.getId());

        verify(postRepository).delete(samplePost);
    }
}

