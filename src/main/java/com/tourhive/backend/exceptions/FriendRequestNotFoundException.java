package com.tourhive.backend.exceptions;

import org.springframework.http.HttpStatus;

public class FriendRequestNotFoundException extends BaseException {
    public FriendRequestNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "FRIEND_REQUEST_NOT_FOUND");
    }
}
