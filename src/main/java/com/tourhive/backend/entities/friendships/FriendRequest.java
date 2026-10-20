package com.tourhive.backend.entities.friendships;

import com.tourhive.backend.entities.BaseEntity;
import com.tourhive.backend.entities.users.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "friend_requests")
public class FriendRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private FriendRequestStatus status = FriendRequestStatus.PENDING;

    @Column(name = "message", length = 500)
    private String message;

    @Builder
    public FriendRequest(User sender, User receiver, FriendRequestStatus status, String message) {
        this.sender = sender;
        this.receiver = receiver;
        this.status = status != null ? status : FriendRequestStatus.PENDING;
        this.message = message;
    }
}
