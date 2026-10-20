package com.tourhive.backend.repositories.notifications;

import com.tourhive.backend.entities.notifications.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {

    Optional<DeviceToken> findByToken(String token);

    List<DeviceToken> findAllByUserId(UUID userId);

    void deleteByTokenAndUserId(String token, UUID userId);

    void deleteAllByUserId(UUID userId);
}
