package com.tourhive.backend.exceptions;

import org.springframework.http.HttpStatus;

public class CannotChatWithSelfException extends BaseException {
    public CannotChatWithSelfException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "CANNOT_CHAT_WITH_SELF");
    }
}
