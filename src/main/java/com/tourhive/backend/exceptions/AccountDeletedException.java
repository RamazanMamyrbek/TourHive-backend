package com.tourhive.backend.exceptions;

import org.springframework.http.HttpStatus;

public class AccountDeletedException extends BaseException {
    public AccountDeletedException(String message) {
        super(message, HttpStatus.FORBIDDEN, "ACCOUNT_DELETED");
    }
}
