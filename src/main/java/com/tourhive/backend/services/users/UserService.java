package com.tourhive.backend.services.users;

import com.tourhive.backend.entities.users.User;

import java.util.UUID;

public interface UserService {

    User getById(UUID id);

    User getByEmail(String email);

    boolean existsByEmail(String email);

    User createPendingUser(String email, String rawPassword);

    User createPendingUser(String email, String rawPassword, String name, String surname);

    User activateUser(User user);

    void updatePassword(User user, String newRawPassword);

    void deleteAccount(UUID userId);
}
