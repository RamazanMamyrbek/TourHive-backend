package com.tourhive.backend.repositories.friendships;

import com.tourhive.backend.entities.friendships.FriendRequest;
import com.tourhive.backend.entities.friendships.FriendRequestStatus;
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
public interface FriendRequestRepository extends JpaRepository<FriendRequest, UUID> {

    Optional<FriendRequest> findBySenderIdAndReceiverIdAndStatus(UUID senderId, UUID receiverId, FriendRequestStatus status);

    boolean existsBySenderIdAndReceiverIdAndStatus(UUID senderId, UUID receiverId, FriendRequestStatus status);

    Page<FriendRequest> findAllByReceiverIdAndStatusOrderByCreatedAtDesc(UUID receiverId, FriendRequestStatus status, Pageable pageable);

    Page<FriendRequest> findAllBySenderIdOrderByCreatedAtDesc(UUID senderId, Pageable pageable);

    @Query("SELECT fr FROM FriendRequest fr WHERE fr.status = :status AND " +
            "((fr.sender.id = :userA AND fr.receiver.id = :userB) OR (fr.sender.id = :userB AND fr.receiver.id = :userA))")
    Optional<FriendRequest> findBetweenUsersAndStatus(
            @Param("userA") UUID userA,
            @Param("userB") UUID userB,
            @Param("status") FriendRequestStatus status
    );

    @Modifying
    @Query("UPDATE FriendRequest fr SET fr.status = 'CANCELLED' WHERE fr.status = 'PENDING' AND " +
            "((fr.sender.id = :userA AND fr.receiver.id = :userB) OR (fr.sender.id = :userB AND fr.receiver.id = :userA))")
    int cancelPendingRequestsBetween(@Param("userA") UUID userA, @Param("userB") UUID userB);
}
