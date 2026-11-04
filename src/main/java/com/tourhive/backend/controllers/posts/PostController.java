package com.tourhive.backend.controllers.posts;

import com.tourhive.backend.configurations.security.UserPrincipal;
import com.tourhive.backend.dto.common.ApiResponseDto;
import com.tourhive.backend.dto.posts.request.CreatePostRequestDto;
import com.tourhive.backend.dto.posts.response.PostResponseDto;
import com.tourhive.backend.services.posts.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Posts", description = "Endpoints for Instagram-style user posts and photo feed")
@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @Operation(
            summary = "Create a new post",
            description = "Publishes a photo post with caption and location",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<ApiResponseDto<PostResponseDto>> createPost(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreatePostRequestDto requestDto
    ) {
        PostResponseDto response = postService.createPost(principal.getId(), requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("Post created successfully", response));
    }

    @Operation(
            summary = "Get user posts",
            description = "Returns paged list of posts created by specific user"
    )
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponseDto<Page<PostResponseDto>>> getUserPosts(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID userId,
            @ParameterObject Pageable pageable
    ) {
        UUID currentUserId = principal != null ? principal.getId() : null;
        Page<PostResponseDto> page = postService.getUserPosts(currentUserId, userId, pageable);
        return ResponseEntity.ok(ApiResponseDto.success(page));
    }

    @Operation(
            summary = "Get posts feed",
            description = "Returns chronological feed of published posts (scope=GLOBAL or scope=FOLLOWING)"
    )
    @GetMapping("/feed")
    public ResponseEntity<ApiResponseDto<Page<PostResponseDto>>> getFeed(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(value = "scope", defaultValue = "GLOBAL") String scope,
            @ParameterObject Pageable pageable
    ) {
        UUID currentUserId = principal != null ? principal.getId() : null;
        Page<PostResponseDto> page = postService.getFeedPosts(currentUserId, scope, pageable);
        return ResponseEntity.ok(ApiResponseDto.success(page));
    }

    @Operation(
            summary = "Toggle like on post",
            description = "Likes or unlikes a post for authenticated user",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{id}/like")
    public ResponseEntity<ApiResponseDto<PostResponseDto>> toggleLike(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        PostResponseDto response = postService.toggleLike(principal.getId(), id);
        return ResponseEntity.ok(ApiResponseDto.success(response));
    }

    @Operation(
            summary = "Delete own post",
            description = "Deletes a post owned by authenticated user",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDto<Void>> deletePost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        postService.deletePost(principal.getId(), id);
        return ResponseEntity.ok(ApiResponseDto.success("Post deleted successfully", null));
    }
}

