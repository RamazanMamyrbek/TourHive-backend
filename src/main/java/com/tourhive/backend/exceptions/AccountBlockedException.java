package com.tourhive.backend.exceptions;

import org.springframework.http.HttpStatus;

public class AccountBlockedException extends BaseException {

    public AccountBlockedException(String message) {
        super(message, HttpStatus.FORBIDDEN, "ACCOUNT_BLOCKED");
    }
}
