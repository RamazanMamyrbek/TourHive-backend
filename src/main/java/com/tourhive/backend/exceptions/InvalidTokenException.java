package com.tourhive.backend.exceptions;

import org.springframework.http.HttpStatus;

public class InvalidTokenException extends BaseException {

    public InvalidTokenException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "INVALID_TOKEN");
    }
}
