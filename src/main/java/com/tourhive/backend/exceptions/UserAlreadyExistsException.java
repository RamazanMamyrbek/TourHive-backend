package com.tourhive.backend.exceptions;

import org.springframework.http.HttpStatus;

public class UserAlreadyExistsException extends BaseException {

    public UserAlreadyExistsException(String message) {
        super(message, HttpStatus.CONFLICT, "USER_ALREADY_EXISTS");
    }
}
