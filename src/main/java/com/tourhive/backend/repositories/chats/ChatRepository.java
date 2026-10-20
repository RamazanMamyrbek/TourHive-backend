package com.tourhive.backend.repositories.chats;

import com.tourhive.backend.entities.chats.Chat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ChatRepository extends JpaRepository<Chat, UUID> {

    Optional<Chat> findByTourId(UUID tourId);

    @Query("SELECT c FROM Chat c WHERE c.type = com.tourhive.backend.entities.chats.ChatType.DIRECT AND ((c.user1.id = :u1 AND c.user2.id = :u2) OR (c.user1.id = :u2 AND c.user2.id = :u1))")
    Optional<Chat> findDirectChatBetween(@Param("u1") UUID u1, @Param("u2") UUID u2);

    @Query("""
        SELECT c FROM Chat c
        JOIN ChatMember cm ON cm.chat.id = c.id
        WHERE cm.user.id = :userId AND cm.isActive = true
        ORDER BY c.updatedAt DESC
    """)
    Page<Chat> findUserChats(@Param("userId") UUID userId, Pageable pageable);
}
