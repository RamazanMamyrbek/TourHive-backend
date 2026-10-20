package com.tourhive.backend.exceptions;

import org.springframework.http.HttpStatus;

public class DuplicateFriendRequestException extends BaseException {
    public DuplicateFriendRequestException(String message) {
        super(message, HttpStatus.CONFLICT, "DUPLICATE_FRIEND_REQUEST");
    }
}
