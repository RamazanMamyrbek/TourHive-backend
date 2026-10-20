package com.tourhive.backend.exceptions;

import org.springframework.http.HttpStatus;

public class FavoriteNotFoundException extends BaseException {
    public FavoriteNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "FAVORITE_NOT_FOUND");
    }
}
