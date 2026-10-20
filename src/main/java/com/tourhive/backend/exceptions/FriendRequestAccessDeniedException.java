package com.tourhive.backend.exceptions;

import org.springframework.http.HttpStatus;

public class FriendRequestAccessDeniedException extends BaseException {
    public FriendRequestAccessDeniedException(String message) {
        super(message, HttpStatus.FORBIDDEN, "FRIEND_REQUEST_ACCESS_DENIED");
    }
}
