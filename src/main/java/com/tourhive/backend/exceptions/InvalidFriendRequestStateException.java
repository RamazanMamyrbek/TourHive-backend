package com.tourhive.backend.exceptions;

import org.springframework.http.HttpStatus;

public class InvalidFriendRequestStateException extends BaseException {
    public InvalidFriendRequestStateException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_FRIEND_REQUEST_STATE");
    }
}
