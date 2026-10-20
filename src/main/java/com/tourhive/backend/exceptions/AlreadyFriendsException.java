package com.tourhive.backend.exceptions;

import org.springframework.http.HttpStatus;

public class AlreadyFriendsException extends BaseException {
    public AlreadyFriendsException(String message) {
        super(message, HttpStatus.CONFLICT, "ALREADY_FRIENDS");
    }
}
