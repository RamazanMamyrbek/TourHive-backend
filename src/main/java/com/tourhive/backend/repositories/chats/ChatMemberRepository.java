package com.tourhive.backend.repositories.chats;

import com.tourhive.backend.entities.chats.ChatMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatMemberRepository extends JpaRepository<ChatMember, UUID> {

    Optional<ChatMember> findByChatIdAndUserId(UUID chatId, UUID userId);

    List<ChatMember> findAllByChatId(UUID chatId);

    List<ChatMember> findAllByChatIdAndIsActiveTrue(UUID chatId);

    boolean existsByChatIdAndUserIdAndIsActiveTrue(UUID chatId, UUID userId);

    long countByChatIdAndIsActiveTrue(UUID chatId);
}
