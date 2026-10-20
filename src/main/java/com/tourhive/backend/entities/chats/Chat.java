package com.tourhive.backend.entities.chats;

import com.tourhive.backend.entities.BaseEntity;
import com.tourhive.backend.entities.tours.Tour;
import com.tourhive.backend.entities.users.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "chats")
public class Chat extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private ChatType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id")
    private Tour tour;

    @Column(name = "title")
    private String title;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id_1")
    private User user1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id_2")
    private User user2;

    public static Chat createDirectChat(User first, User second, User creator) {
        User u1 = first;
        User u2 = second;
        if (first.getId().toString().compareTo(second.getId().toString()) > 0) {
            u1 = second;
            u2 = first;
        }
        return Chat.builder()
                .type(ChatType.DIRECT)
                .user1(u1)
                .user2(u2)
                .createdBy(creator)
                .build();
    }

    public static Chat createTourGroupChat(Tour tour, User organizer) {
        return Chat.builder()
                .type(ChatType.TOUR_GROUP)
                .tour(tour)
                .title(tour.getTitle())
                .avatarUrl(tour.getCoverImageUrl())
                .createdBy(organizer)
                .build();
    }
}
