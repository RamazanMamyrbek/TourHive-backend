package com.tourhive.backend.services.posts.impl;

import com.tourhive.backend.dto.posts.request.CreatePostRequestDto;
import com.tourhive.backend.dto.posts.response.PostResponseDto;
import com.tourhive.backend.entities.posts.Post;
import com.tourhive.backend.entities.posts.PostLike;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserProfile;
import com.tourhive.backend.exceptions.ResourceNotFoundException;
import com.tourhive.backend.repositories.posts.PostLikeRepository;
import com.tourhive.backend.repositories.posts.PostRepository;
import com.tourhive.backend.repositories.users.UserProfileRepository;
import com.tourhive.backend.repositories.users.UserRepository;
import com.tourhive.backend.services.posts.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    @Override
    @Transactional
    public PostResponseDto createPost(UUID currentUserId, CreatePostRequestDto request) {
        User author = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUserId));

        Post post = Post.builder()
                .author(author)
                .imageUrl(request.imageUrl().trim())
                .caption(request.caption() != null ? request.caption().trim() : null)
                .location(request.location() != null ? request.location().trim() : null)
                .likesCount(0)
                .build();

        Post saved = postRepository.save(post);
        log.info("User {} created new post {}", currentUserId, saved.getId());
        return toDto(saved, currentUserId);
    }

    @Override
    public Page<PostResponseDto> getUserPosts(UUID currentUserId, UUID targetUserId, Pageable pageable) {
        return postRepository.findAllByAuthorIdOrderByCreatedAtDesc(targetUserId, pageable)
                .map(post -> toDto(post, currentUserId));
    }

    @Override
    public Page<PostResponseDto> getFeedPosts(UUID currentUserId, Pageable pageable) {
        return postRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(post -> toDto(post, currentUserId));
    }

    @Override
    @Transactional
    public PostResponseDto toggleLike(UUID currentUserId, UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found: " + postId));

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUserId));

        Optional<PostLike> existingLike = postLikeRepository.findByPostIdAndUserId(postId, currentUserId);
        if (existingLike.isPresent()) {
            postLikeRepository.delete(existingLike.get());
            post.setLikesCount(Math.max(0, post.getLikesCount() - 1));
            log.info("User {} unliked post {}", currentUserId, postId);
        } else {
            PostLike newLike = PostLike.builder()
                    .post(post)
                    .user(user)
                    .build();
            postLikeRepository.save(newLike);
            post.setLikesCount(post.getLikesCount() + 1);
            log.info("User {} liked post {}", currentUserId, postId);
        }

        Post updated = postRepository.save(post);
        return toDto(updated, currentUserId);
    }

    @Override
    @Transactional
    public void deletePost(UUID currentUserId, UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found: " + postId));

        if (!post.getAuthor().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You are not the author of this post");
        }

        postRepository.delete(post);
        log.info("User {} deleted post {}", currentUserId, postId);
    }

    private PostResponseDto toDto(Post post, UUID currentUserId) {
        User author = post.getAuthor();
        UserProfile profile = userProfileRepository.findByUserId(author.getId()).orElse(null);

        String authorName = profile != null && profile.getFullName() != null
                ? profile.getFullName()
                : (author.getEmail() != null ? author.getEmail() : "User");

        String authorNickname = profile != null ? profile.getNickname() : null;
        String authorAvatarUrl = profile != null ? profile.getAvatarUrl() : null;

        boolean isLikedByMe = currentUserId != null
                && postLikeRepository.existsByPostIdAndUserId(post.getId(), currentUserId);

        return new PostResponseDto(
                post.getId(),
                author.getId(),
                authorName,
                authorNickname,
                authorAvatarUrl,
                post.getImageUrl(),
                post.getCaption(),
                post.getLocation(),
                post.getLikesCount() != null ? post.getLikesCount() : 0,
                isLikedByMe,
                post.getCreatedAt()
        );
    }
}

