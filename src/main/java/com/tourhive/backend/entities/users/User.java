package com.tourhive.backend.entities.users;

import com.tourhive.backend.entities.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private UserStatus status = UserStatus.PENDING_VERIFICATION;

    @Column(name = "is_organizer", nullable = false)
    private boolean isOrganizer = false;

    @Column(name = "is_business_verified", nullable = false)
    private boolean isBusinessVerified = false;

    public User() {
    }

    public User(
            String email,
            String passwordHash,
            Role role,
            UserStatus status,
            boolean isOrganizer,
            boolean isBusinessVerified
    ) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role != null ? role : Role.USER;
        this.status = status != null ? status : UserStatus.PENDING_VERIFICATION;
        this.isOrganizer = isOrganizer;
        this.isBusinessVerified = isBusinessVerified;
    }

    public static UserBuilder builder() {
        return new UserBuilder();
    }

    public static class UserBuilder {
        private String email;
        private String passwordHash;
        private Role role = Role.USER;
        private UserStatus status = UserStatus.PENDING_VERIFICATION;
        private boolean isOrganizer = false;
        private boolean isBusinessVerified = false;

        public UserBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserBuilder passwordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        public UserBuilder role(Role role) {
            this.role = role != null ? role : Role.USER;
            return this;
        }

        public UserBuilder status(UserStatus status) {
            this.status = status != null ? status : UserStatus.PENDING_VERIFICATION;
            return this;
        }

        public UserBuilder isOrganizer(boolean isOrganizer) {
            this.isOrganizer = isOrganizer;
            return this;
        }

        public UserBuilder isBusinessVerified(boolean isBusinessVerified) {
            this.isBusinessVerified = isBusinessVerified;
            return this;
        }

        public User build() {
            return new User(email, passwordHash, role, status, isOrganizer, isBusinessVerified);
        }
    }
}
