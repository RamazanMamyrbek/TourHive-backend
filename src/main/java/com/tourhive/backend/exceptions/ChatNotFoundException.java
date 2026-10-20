package com.tourhive.backend.exceptions;

import org.springframework.http.HttpStatus;

public class ChatNotFoundException extends BaseException {
    public ChatNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "CHAT_NOT_FOUND");
    }
}
