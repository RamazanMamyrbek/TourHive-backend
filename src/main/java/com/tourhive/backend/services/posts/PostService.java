package com.tourhive.backend.services.posts;

import com.tourhive.backend.dto.posts.request.CreatePostRequestDto;
import com.tourhive.backend.dto.posts.response.PostResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PostService {

    PostResponseDto createPost(UUID currentUserId, CreatePostRequestDto request);

    Page<PostResponseDto> getUserPosts(UUID currentUserId, UUID targetUserId, Pageable pageable);

    Page<PostResponseDto> getFeedPosts(UUID currentUserId, Pageable pageable);

    PostResponseDto toggleLike(UUID currentUserId, UUID postId);

    void deletePost(UUID currentUserId, UUID postId);
}

