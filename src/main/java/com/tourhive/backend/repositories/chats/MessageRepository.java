package com.tourhive.backend.repositories.chats;

import com.tourhive.backend.entities.chats.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    Page<Message> findAllByChatIdOrderByCreatedAtDesc(UUID chatId, Pageable pageable);

    Page<Message> findAllByChatIdAndCreatedAtLessThanEqualOrderByCreatedAtDesc(UUID chatId, Instant leftAt, Pageable pageable);

    Optional<Message> findFirstByChatIdOrderByCreatedAtDesc(UUID chatId);

    @Query("""
        SELECT count(m) FROM Message m
        WHERE m.chat.id = :chatId
          AND m.sender.id <> :userId
          AND NOT EXISTS (
              SELECT 1 FROM MessageRead mr
              WHERE mr.message.id = m.id AND mr.user.id = :userId
          )
    """)
    long countUnreadMessages(@Param("chatId") UUID chatId, @Param("userId") UUID userId);

    @Query("""
        SELECT count(m) FROM Message m
        WHERE m.chat.id = :chatId
          AND m.sender.id <> :userId
          AND m.createdAt <= :leftAt
          AND NOT EXISTS (
              SELECT 1 FROM MessageRead mr
              WHERE mr.message.id = m.id AND mr.user.id = :userId
          )
    """)
    long countUnreadMessagesBefore(@Param("chatId") UUID chatId, @Param("userId") UUID userId, @Param("leftAt") Instant leftAt);

    @Query("""
        SELECT m FROM Message m
        WHERE m.chat.id = :chatId
          AND m.sender.id <> :userId
          AND NOT EXISTS (
              SELECT 1 FROM MessageRead mr
              WHERE mr.message.id = m.id AND mr.user.id = :userId
          )
    """)
    List<Message> findUnreadMessages(
            @Param("chatId") UUID chatId,
            @Param("userId") UUID userId
    );

    @Query("""
        SELECT m FROM Message m
        WHERE m.chat.id = :chatId
          AND m.sender.id <> :userId
          AND m.createdAt <= :lastMessageCreatedAt
          AND NOT EXISTS (
              SELECT 1 FROM MessageRead mr
              WHERE mr.message.id = m.id AND mr.user.id = :userId
          )
    """)
    List<Message> findUnreadMessagesUpTo(
            @Param("chatId") UUID chatId,
            @Param("userId") UUID userId,
            @Param("lastMessageCreatedAt") Instant lastMessageCreatedAt
    );
}
