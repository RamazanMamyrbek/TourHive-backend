package com.tourhive.backend.controllers.posts;

import com.tourhive.backend.configurations.security.UserPrincipal;
import com.tourhive.backend.dto.common.ApiResponseDto;
import com.tourhive.backend.dto.posts.request.CreatePostCommentRequestDto;
import com.tourhive.backend.dto.posts.response.PostCommentResponseDto;
import com.tourhive.backend.services.posts.PostCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Post Comments", description = "Endpoints for managing comments on user posts")
@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostCommentController {

    private final PostCommentService postCommentService;

    @Operation(summary = "Add comment to post", description = "Creates a new comment on a specified post", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{postId}/comments")
    public ResponseEntity<ApiResponseDto<PostCommentResponseDto>> addComment(
            @PathVariable UUID postId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreatePostCommentRequestDto requestDto
    ) {
        PostCommentResponseDto comment = postCommentService.addComment(principal.getId(), postId, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("Comment added successfully", comment));
    }

    @Operation(summary = "Get post comments", description = "Returns paginated comments for a post in chronological order")
    @GetMapping("/{postId}/comments")
    public ResponseEntity<ApiResponseDto<Page<PostCommentResponseDto>>> getComments(
            @PathVariable UUID postId,
            @AuthenticationPrincipal UserPrincipal principal,
            @ParameterObject @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        UUID currentUserId = principal != null ? principal.getId() : null;
        Page<PostCommentResponseDto> comments = postCommentService.getComments(currentUserId, postId, pageable);
        return ResponseEntity.ok(ApiResponseDto.success(comments));
    }

    @Operation(summary = "Delete comment", description = "Deletes a comment by comment author or post author", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable UUID postId,
            @PathVariable UUID commentId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        postCommentService.deleteComment(principal.getId(), commentId);
        return ResponseEntity.noContent().build();
    }
}

