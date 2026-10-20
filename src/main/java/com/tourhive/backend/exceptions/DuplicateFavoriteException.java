package com.tourhive.backend.exceptions;

import org.springframework.http.HttpStatus;

public class DuplicateFavoriteException extends BaseException {
    public DuplicateFavoriteException(String message) {
        super(message, HttpStatus.CONFLICT, "DUPLICATE_FAVORITE");
    }
}
