package com.tourhive.backend.repositories.friendships;

import com.tourhive.backend.entities.friendships.Friendship;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {

    Optional<Friendship> findByUser1IdAndUser2Id(UUID user1Id, UUID user2Id);

    boolean existsByUser1IdAndUser2Id(UUID user1Id, UUID user2Id);

    @Query("SELECT f FROM Friendship f WHERE f.user1.id = :userId OR f.user2.id = :userId ORDER BY f.createdAt DESC")
    Page<Friendship> findAllByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM Friendship f WHERE (f.user1.id = :u1 AND f.user2.id = :u2) OR (f.user1.id = :u2 AND f.user2.id = :u1)")
    int deleteFriendshipBetween(@Param("u1") UUID u1, @Param("u2") UUID u2);

    default boolean areFriends(UUID userA, UUID userB) {
        boolean aFirst = userA.toString().compareTo(userB.toString()) < 0;
        UUID u1 = aFirst ? userA : userB;
        UUID u2 = aFirst ? userB : userA;
        return existsByUser1IdAndUser2Id(u1, u2);
    }
}
