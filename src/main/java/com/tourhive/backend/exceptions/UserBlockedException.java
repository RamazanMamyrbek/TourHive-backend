package com.tourhive.backend.exceptions;

import org.springframework.http.HttpStatus;

public class UserBlockedException extends BaseException {
    public UserBlockedException(String message) {
        super(message, HttpStatus.FORBIDDEN, "USER_BLOCKED");
    }
}
