package com.tourhive.backend.entities.users;

import com.tourhive.backend.entities.BaseEntity;
import com.tourhive.backend.entities.resources.Resource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "user_profiles")
public class UserProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "nickname", length = 50, unique = true)
    private String nickname;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "avatar_resource_id")
    private Resource avatarResource;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "languages")
    private String languages;

    @Column(name = "phone", length = 50)
    private String phone;

    public String getFullName() {
        if (firstName == null && lastName == null) {
            return null;
        }
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }
        return firstName != null ? firstName : lastName;
    }

    public UserProfile() {
    }

    @Builder
    public UserProfile(
            User user,
            String firstName,
            String lastName,
            String nickname,
            Resource avatarResource,
            String avatarUrl,
            String bio,
            String country,
            String city,
            String languages,
            String phone
    ) {
        this.user = user;
        this.firstName = firstName;
        this.lastName = lastName;
        this.nickname = nickname;
        this.avatarResource = avatarResource;
        this.avatarUrl = avatarUrl;
        this.bio = bio;
        this.country = country;
        this.city = city;
        this.languages = languages;
        this.phone = phone;
    }
}
