package com.tourhive.backend.entities.friendships;

import com.tourhive.backend.entities.BaseEntity;
import com.tourhive.backend.entities.users.User;
import jakarta.persistence.Entity;
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
@Table(name = "friendships")
public class Friendship extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id_1", nullable = false)
    private User user1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id_2", nullable = false)
    private User user2;

    @Builder
    public Friendship(User user1, User user2) {
        if (user1 != null && user2 != null && user1.getId() != null && user2.getId() != null) {
            if (user1.getId().toString().compareTo(user2.getId().toString()) > 0) {
                this.user1 = user2;
                this.user2 = user1;
                return;
            }
        }
        this.user1 = user1;
        this.user2 = user2;
    }
}
