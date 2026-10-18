package com.tourhive.backend.repositories.users;

import com.tourhive.backend.entities.users.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    Optional<UserProfile> findByUserId(UUID userId);

    Optional<UserProfile> findByNickname(String nickname);

    boolean existsByNickname(String nickname);

    boolean existsByNicknameAndUserIdNot(String nickname, UUID userId);
}
