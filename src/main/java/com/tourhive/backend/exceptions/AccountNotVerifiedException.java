package com.tourhive.backend.exceptions;

import org.springframework.http.HttpStatus;

public class AccountNotVerifiedException extends BaseException {

    public AccountNotVerifiedException(String message) {
        super(message, HttpStatus.FORBIDDEN, "ACCOUNT_NOT_VERIFIED");
    }
}
