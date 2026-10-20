package com.tourhive.backend.exceptions;

import org.springframework.http.HttpStatus;

public class InvalidApplicationStateException extends BaseException {

    public InvalidApplicationStateException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_APPLICATION_STATE");
    }
}
