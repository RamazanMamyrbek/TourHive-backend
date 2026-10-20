package com.tourhive.backend.exceptions;

import org.springframework.http.HttpStatus;

public class CannotBefriendSelfException extends BaseException {
    public CannotBefriendSelfException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "CANNOT_BEFRIEND_SELF");
    }
}
