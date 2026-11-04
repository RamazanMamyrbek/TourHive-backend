package com.tourhive.backend.repositories.posts;

import com.tourhive.backend.entities.posts.PostComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PostCommentRepository extends JpaRepository<PostComment, UUID> {

    @Query("SELECT c FROM PostComment c JOIN FETCH c.author WHERE c.post.id = :postId ORDER BY c.createdAt ASC")
    Page<PostComment> findAllByPostIdOrderByCreatedAtAsc(@Param("postId") UUID postId, Pageable pageable);

    long countByPostId(UUID postId);

    void deleteAllByPostId(UUID postId);
}
