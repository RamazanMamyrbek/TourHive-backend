package com.tourhive.backend.exceptions;

import org.springframework.http.HttpStatus;

public class FriendshipNotFoundException extends BaseException {
    public FriendshipNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "FRIENDSHIP_NOT_FOUND");
    }
}
