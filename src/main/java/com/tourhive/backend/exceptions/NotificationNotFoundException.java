package com.tourhive.backend.exceptions;

import org.springframework.http.HttpStatus;

public class NotificationNotFoundException extends BaseException {
    public NotificationNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND");
    }
}
