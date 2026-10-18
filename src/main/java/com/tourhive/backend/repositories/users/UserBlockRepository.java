package com.tourhive.backend.repositories.users;

import com.tourhive.backend.entities.users.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserBlockRepository extends JpaRepository<UserBlock, UUID> {

    Optional<UserBlock> findByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);

    boolean existsByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);

    List<UserBlock> findAllByBlockerIdOrderByCreatedAtDesc(UUID blockerId);

    @Query("SELECT COUNT(ub) > 0 FROM UserBlock ub WHERE " +
            "(ub.blocker.id = :userA AND ub.blocked.id = :userB) OR " +
            "(ub.blocker.id = :userB AND ub.blocked.id = :userA)")
    boolean isBlockedBetween(@Param("userA") UUID userA, @Param("userB") UUID userB);
}
