package com.tourhive.backend.exceptions;

import org.springframework.http.HttpStatus;

public class ChatAccessDeniedException extends BaseException {
    public ChatAccessDeniedException(String message) {
        super(message, HttpStatus.FORBIDDEN, "CHAT_ACCESS_DENIED");
    }
}
