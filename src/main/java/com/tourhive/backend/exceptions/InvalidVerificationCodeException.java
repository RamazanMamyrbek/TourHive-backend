package com.tourhive.backend.exceptions;

import org.springframework.http.HttpStatus;

public class InvalidVerificationCodeException extends BaseException {

    public InvalidVerificationCodeException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_VERIFICATION_CODE");
    }
}
