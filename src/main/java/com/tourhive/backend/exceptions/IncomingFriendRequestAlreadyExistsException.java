package com.tourhive.backend.exceptions;

import org.springframework.http.HttpStatus;

public class IncomingFriendRequestAlreadyExistsException extends BaseException {
    public IncomingFriendRequestAlreadyExistsException(String message) {
        super(message, HttpStatus.CONFLICT, "INCOMING_REQUEST_ALREADY_EXISTS");
    }
}
