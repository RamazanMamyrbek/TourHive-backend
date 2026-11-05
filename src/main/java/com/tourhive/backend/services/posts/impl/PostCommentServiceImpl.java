package com.tourhive.backend.services.posts.impl;

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
import com.tourhive.backend.services.posts.PostCommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostCommentServiceImpl implements PostCommentService {

    private final PostCommentRepository postCommentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    @Override
    @Transactional
    public PostCommentResponseDto addComment(UUID currentUserId, UUID postId, CreatePostCommentRequestDto requestDto) {
        User author = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUserId));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found: " + postId));

        PostComment comment = PostComment.builder()
                .post(post)
                .author(author)
                .content(requestDto.content().trim())
                .build();

        PostComment saved = postCommentRepository.save(comment);

        post.setCommentsCount(post.getCommentsCount() + 1);
        postRepository.save(post);

        log.info("User {} commented on post {}: comment {}", currentUserId, postId, saved.getId());
        return toDto(saved, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostCommentResponseDto> getComments(UUID currentUserId, UUID postId, Pageable pageable) {
        if (!postRepository.existsById(postId)) {
            throw new ResourceNotFoundException("Post not found: " + postId);
        }

        Page<PostComment> comments = postCommentRepository.findAllByPostIdOrderByCreatedAtAsc(postId, pageable);
        return comments.map(comment -> toDto(comment, currentUserId));
    }

    @Override
    @Transactional
    public void deleteComment(UUID currentUserId, UUID commentId) {
        PostComment comment = postCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found: " + commentId));

        Post post = comment.getPost();
        boolean isCommentAuthor = comment.getAuthor().getId().equals(currentUserId);
        boolean isPostAuthor = post.getAuthor().getId().equals(currentUserId);

        if (!isCommentAuthor && !isPostAuthor) {
            throw new AccessDeniedException("You are not authorized to delete this comment");
        }

        postCommentRepository.delete(comment);

        if (post.getCommentsCount() > 0) {
            post.setCommentsCount(post.getCommentsCount() - 1);
            postRepository.save(post);
        }

        log.info("User {} deleted comment {} from post {}", currentUserId, commentId, post.getId());
    }

    private PostCommentResponseDto toDto(PostComment comment, UUID currentUserId) {
        User author = comment.getAuthor();
        UserProfile profile = userProfileRepository.findByUserId(author.getId()).orElse(null);

        String authorName = profile != null && profile.getFullName() != null && !profile.getFullName().isBlank()
                ? profile.getFullName()
                : author.getEmail();
        String authorNickname = profile != null ? profile.getNickname() : null;
        String authorAvatarUrl = profile != null ? profile.getAvatarUrl() : null;
        boolean isMyComment = currentUserId != null && currentUserId.equals(author.getId());

        return new PostCommentResponseDto(
                comment.getId(),
                comment.getPost().getId(),
                author.getId(),
                authorName,
                authorNickname,
                authorAvatarUrl,
                comment.getContent(),
                isMyComment,
                comment.getCreatedAt()
        );
    }
}

