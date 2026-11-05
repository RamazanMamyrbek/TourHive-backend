package com.tourhive.backend.repositories.posts;

import com.tourhive.backend.entities.posts.PostMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PostMediaRepository extends JpaRepository<PostMedia, UUID> {

    List<PostMedia> findAllByPostIdOrderBySortOrderAsc(UUID postId);

    void deleteAllByPostId(UUID postId);
}

