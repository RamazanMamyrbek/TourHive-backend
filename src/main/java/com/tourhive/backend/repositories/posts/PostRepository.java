package com.tourhive.backend.repositories.posts;

import com.tourhive.backend.entities.posts.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {

    Page<Post> findAllByAuthorIdOrderByCreatedAtDesc(UUID authorId, Pageable pageable);

    Page<Post> findAllByAuthorIdAndIsPrivateFalseOrderByCreatedAtDesc(UUID authorId, Pageable pageable);

    Page<Post> findAllByIsPrivateFalseOrderByCreatedAtDesc(Pageable pageable);

    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.author.id IN :authorIds ORDER BY p.createdAt DESC")
    Page<Post> findAllByAuthorIdsOrderByCreatedAtDesc(@Param("authorIds") Collection<UUID> authorIds, Pageable pageable);

    long countByAuthorId(UUID authorId);

    long countByAuthorIdAndIsPrivateFalse(UUID authorId);
}

