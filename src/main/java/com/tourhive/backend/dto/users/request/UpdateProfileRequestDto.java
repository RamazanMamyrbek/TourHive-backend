package com.tourhive.backend.dto.users.request;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequestDto(
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        @Size(min = 3, max = 50, message = "Nickname must be between 3 and 50 characters")
        String nickname,

        String bio,

        @Size(max = 100, message = "Country must not exceed 100 characters")
        String country,

        @Size(max = 100, message = "City must not exceed 100 characters")
        String city,

        @Size(max = 255, message = "Languages must not exceed 255 characters")
        String languages,

        @Size(max = 50, message = "Phone must not exceed 50 characters")
        String phone,

        @Size(max = 100, message = "Telegram handle must not exceed 100 characters")
        String telegram,

        @Size(max = 50, message = "WhatsApp number must not exceed 50 characters")
        String whatsapp,

        @Size(max = 100, message = "Instagram handle must not exceed 100 characters")
        String instagram,

        @Size(max = 150, message = "Contact email must not exceed 150 characters")
        String contactEmail,

        @Size(max = 150, message = "Specialization must not exceed 150 characters")
        String specialization,

        Integer experienceYears,

        @Size(max = 255, message = "Website must not exceed 255 characters")
        String website
) {
    public UpdateProfileRequestDto(
            String firstName,
            String lastName,
            String nickname,
            String bio,
            String country,
            String city,
            String languages,
            String phone
    ) {
        this(firstName, lastName, nickname, bio, country, city, languages, phone, null, null, null, null, null, null, null);
    }
}
