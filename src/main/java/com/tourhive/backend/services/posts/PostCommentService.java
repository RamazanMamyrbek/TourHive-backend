package com.tourhive.backend.services.posts;

import com.tourhive.backend.dto.posts.request.CreatePostCommentRequestDto;
import com.tourhive.backend.dto.posts.response.PostCommentResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PostCommentService {

    PostCommentResponseDto addComment(UUID currentUserId, UUID postId, CreatePostCommentRequestDto requestDto);

    Page<PostCommentResponseDto> getComments(UUID currentUserId, UUID postId, Pageable pageable);

    void deleteComment(UUID currentUserId, UUID commentId);
}
