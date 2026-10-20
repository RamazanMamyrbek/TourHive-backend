package com.tourhive.backend.exceptions;

import org.springframework.http.HttpStatus;

public class InvalidMessageContentException extends BaseException {
    public InvalidMessageContentException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_MESSAGE_CONTENT");
    }
}
