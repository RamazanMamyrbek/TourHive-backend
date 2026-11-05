package com.tourhive.backend.posts;

import com.tourhive.backend.dto.posts.request.CreatePostCommentRequestDto;
import com.tourhive.backend.dto.posts.response.PostCommentResponseDto;
import com.tourhive.backend.entities.posts.Post;
import com.tourhive.backend.entities.posts.PostComment;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserProfile;
import com.tourhive.backend.exceptions.ResourceNotFoundException;
import com.tourhive.backend.repositories.posts.PostCommentRepository;
import com.tourhive.backend.repositories.posts.PostRepository;
import com.tourhive.backend.repositories.users.UserProfileRepository;
import com.tourhive.backend.repositories.users.UserRepository;
import com.tourhive.backend.services.posts.impl.PostCommentServiceImpl;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostCommentServiceTest {

    @Mock
    private PostCommentRepository postCommentRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @InjectMocks
    private PostCommentServiceImpl postCommentService;

    private User postAuthor;
    private User commenter;
    private User stranger;
    private Post post;
    private PostComment comment;

    @BeforeEach
    void setUp() {
        postAuthor = User.builder().email("author@example.com").build();
        postAuthor.setId(UUID.randomUUID());

        commenter = User.builder().email("commenter@example.com").build();
        commenter.setId(UUID.randomUUID());

        stranger = User.builder().email("stranger@example.com").build();
        stranger.setId(UUID.randomUUID());

        post = Post.builder()
                .author(postAuthor)
                .imageUrl("https://example.com/image.jpg")
                .caption("Hello world")
                .likesCount(0)
                .commentsCount(0)
                .build();
        post.setId(UUID.randomUUID());

        comment = PostComment.builder()
                .post(post)
                .author(commenter)
                .content("Nice photo!")
                .build();
        comment.setId(UUID.randomUUID());
        comment.setCreatedAt(Instant.now());
    }

    @Test
    @DisplayName("Successfully add comment to post")
    void addComment_success() {
        CreatePostCommentRequestDto request = new CreatePostCommentRequestDto("Awesome post!");

        when(userRepository.findById(commenter.getId())).thenReturn(Optional.of(commenter));
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
        when(postCommentRepository.save(any(PostComment.class))).thenAnswer(inv -> {
            PostComment c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            c.setCreatedAt(Instant.now());
            return c;
        });
        when(userProfileRepository.findByUserId(commenter.getId())).thenReturn(Optional.empty());

        PostCommentResponseDto result = postCommentService.addComment(commenter.getId(), post.getId(), request);

        assertThat(result).isNotNull();
        assertThat(result.content()).isEqualTo("Awesome post!");
        assertThat(result.isMyComment()).isTrue();
        assertThat(post.getCommentsCount()).isEqualTo(1);

        verify(postCommentRepository).save(any(PostComment.class));
        verify(postRepository).save(post);
    }

    @Test
    @DisplayName("Adding comment throws when post not found")
    void addComment_postNotFound() {
        CreatePostCommentRequestDto request = new CreatePostCommentRequestDto("Awesome post!");

        when(userRepository.findById(commenter.getId())).thenReturn(Optional.of(commenter));
        when(postRepository.findById(post.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postCommentService.addComment(commenter.getId(), post.getId(), request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Get comments for post")
    void getComments_success() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(postRepository.existsById(post.getId())).thenReturn(true);
        when(postCommentRepository.findAllByPostIdOrderByCreatedAtAsc(post.getId(), pageable))
                .thenReturn(new PageImpl<>(List.of(comment)));
        when(userProfileRepository.findByUserId(commenter.getId())).thenReturn(Optional.empty());

        Page<PostCommentResponseDto> result = postCommentService.getComments(commenter.getId(), post.getId(), pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).content()).isEqualTo("Nice photo!");
        assertThat(result.getContent().get(0).isMyComment()).isTrue();
    }

    @Test
    @DisplayName("Delete comment by comment author succeeds")
    void deleteComment_byAuthor_success() {
        post.setCommentsCount(1);
        when(postCommentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));

        postCommentService.deleteComment(commenter.getId(), comment.getId());

        assertThat(post.getCommentsCount()).isEqualTo(0);
        verify(postCommentRepository).delete(comment);
        verify(postRepository).save(post);
    }

    @Test
    @DisplayName("Delete comment by post author succeeds")
    void deleteComment_byPostAuthor_success() {
        post.setCommentsCount(1);
        when(postCommentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));

        postCommentService.deleteComment(postAuthor.getId(), comment.getId());

        assertThat(post.getCommentsCount()).isEqualTo(0);
        verify(postCommentRepository).delete(comment);
        verify(postRepository).save(post);
    }

    @Test
    @DisplayName("Delete comment by stranger throws AccessDeniedException")
    void deleteComment_byStranger_throwsAccessDenied() {
        when(postCommentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> postCommentService.deleteComment(stranger.getId(), comment.getId()))
                .isInstanceOf(AccessDeniedException.class);

        verify(postCommentRepository, never()).delete(any());
    }
}

