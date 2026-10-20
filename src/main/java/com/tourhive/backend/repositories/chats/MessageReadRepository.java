package com.tourhive.backend.repositories.chats;

import com.tourhive.backend.entities.chats.MessageRead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MessageReadRepository extends JpaRepository<MessageRead, UUID> {

    boolean existsByMessageIdAndUserId(UUID messageId, UUID userId);

    int countByMessageId(UUID messageId);
}
