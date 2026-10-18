package com.tourhive.backend.services.users.impl;

import com.tourhive.backend.entities.users.Role;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserStatus;
import com.tourhive.backend.exceptions.ResourceNotFoundException;
import com.tourhive.backend.exceptions.UserAlreadyExistsException;
import com.tourhive.backend.repositories.users.UserRepository;
import com.tourhive.backend.services.users.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User getById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Override
    public User getByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email.toLowerCase().trim());
    }

    @Override
    @Transactional
    public User createPendingUser(String email, String rawPassword) {
        String normalizedEmail = email.toLowerCase().trim();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new UserAlreadyExistsException("User with email " + normalizedEmail + " already exists");
        }

        User user = User.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(Role.USER)
                .status(UserStatus.PENDING_VERIFICATION)
                .isOrganizer(false)
                .isBusinessVerified(false)
                .build();

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User activateUser(User user) {
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void updatePassword(User user, String newRawPassword) {
        user.setPasswordHash(passwordEncoder.encode(newRawPassword));
        userRepository.save(user);
    }
}
