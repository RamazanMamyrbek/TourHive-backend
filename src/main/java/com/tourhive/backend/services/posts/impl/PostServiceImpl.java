package com.tourhive.backend.services.posts.impl;

import com.tourhive.backend.dto.posts.request.CreatePostRequestDto;
import com.tourhive.backend.dto.posts.response.PostResponseDto;
import com.tourhive.backend.dto.posts.response.PostMediaResponseDto;
import com.tourhive.backend.entities.posts.Post;
import com.tourhive.backend.entities.posts.PostLike;
import com.tourhive.backend.entities.posts.PostMedia;
import com.tourhive.backend.entities.posts.PostMediaType;
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
    private final com.tourhive.backend.repositories.users.UserFollowRepository userFollowRepository;

    @Override
    @Transactional
    public PostResponseDto createPost(UUID currentUserId, CreatePostRequestDto request) {
        User author = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUserId));

        String primaryImageUrl = "";
        if (request.imageUrl() != null && !request.imageUrl().isBlank()) {
            primaryImageUrl = request.imageUrl().trim();
        } else if (request.media() != null && !request.media().isEmpty() && request.media().get(0).mediaUrl() != null) {
            primaryImageUrl = request.media().get(0).mediaUrl().trim();
        }

        Post post = Post.builder()
                .author(author)
                .imageUrl(primaryImageUrl)
                .caption(request.caption() != null ? request.caption().trim() : null)
                .location(request.location() != null ? request.location().trim() : null)
                .isPrivate(Boolean.TRUE.equals(request.isPrivate()))
                .likesCount(0)
                .commentsCount(0)
                .build();

        if (request.media() != null && !request.media().isEmpty()) {
            int order = 0;
            for (var m : request.media()) {
                if (m.mediaUrl() != null && !m.mediaUrl().isBlank()) {
                    PostMediaType type = "VIDEO".equalsIgnoreCase(m.mediaType()) ? PostMediaType.VIDEO : PostMediaType.IMAGE;
                    post.addMedia(PostMedia.builder()
                            .mediaUrl(m.mediaUrl().trim())
                            .mediaType(type)
                            .sortOrder(order++)
                            .build());
                }
            }
        } else if (!primaryImageUrl.isBlank()) {
            post.addMedia(PostMedia.builder()
                    .mediaUrl(primaryImageUrl)
                    .mediaType(PostMediaType.IMAGE)
                    .sortOrder(0)
                    .build());
        }

        Post saved = postRepository.save(post);
        log.info("User {} created new post {}", currentUserId, saved.getId());
        return toDto(saved, currentUserId);
    }

    @Override
    public Page<PostResponseDto> getUserPosts(UUID currentUserId, UUID targetUserId, Pageable pageable) {
        boolean canSeePrivate = currentUserId != null &&
                (currentUserId.equals(targetUserId) || userFollowRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId));

        Page<Post> postsPage = canSeePrivate
                ? postRepository.findAllByAuthorIdOrderByCreatedAtDesc(targetUserId, pageable)
                : postRepository.findAllByAuthorIdAndIsPrivateFalseOrderByCreatedAtDesc(targetUserId, pageable);

        return postsPage.map(post -> toDto(post, currentUserId));
    }

    @Override
    public Page<PostResponseDto> getFeedPosts(UUID currentUserId, Pageable pageable) {
        return getFeedPosts(currentUserId, "GLOBAL", pageable);
    }

    @Override
    public Page<PostResponseDto> getFeedPosts(UUID currentUserId, String scope, Pageable pageable) {
        if ("FOLLOWING".equalsIgnoreCase(scope)) {
            if (currentUserId == null) {
                return Page.empty(pageable);
            }
            java.util.List<UUID> authorIds = new java.util.ArrayList<>(userFollowRepository.findFollowingIdsByFollowerId(currentUserId));
            authorIds.add(currentUserId);
            if (authorIds.isEmpty()) {
                return Page.empty(pageable);
            }
            return postRepository.findAllByAuthorIdsOrderByCreatedAtDesc(authorIds, pageable)
                    .map(post -> toDto(post, currentUserId));
        }

        return postRepository.findAllByIsPrivateFalseOrderByCreatedAtDesc(pageable)
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

        java.util.List<PostMediaResponseDto> mediaList = new java.util.ArrayList<>();
        if (post.getMedia() != null && !post.getMedia().isEmpty()) {
            for (var m : post.getMedia()) {
                mediaList.add(new PostMediaResponseDto(
                        m.getId(),
                        m.getMediaUrl(),
                        m.getMediaType() != null ? m.getMediaType().name() : "IMAGE"
                ));
            }
        } else if (post.getImageUrl() != null && !post.getImageUrl().isBlank()) {
            mediaList.add(new PostMediaResponseDto(null, post.getImageUrl(), "IMAGE"));
        }

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
                post.getCommentsCount() != null ? post.getCommentsCount() : 0,
                isLikedByMe,
                Boolean.TRUE.equals(post.getIsPrivate()),
                mediaList,
                post.getCreatedAt()
        );
    }
}

