package com.tourhive.backend.exceptions;

import org.springframework.http.HttpStatus;

public class CannotFollowSelfException extends BaseException {
    public CannotFollowSelfException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "CANNOT_FOLLOW_SELF");
    }
}
