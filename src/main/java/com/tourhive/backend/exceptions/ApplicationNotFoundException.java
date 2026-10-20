package com.tourhive.backend.exceptions;

import org.springframework.http.HttpStatus;

public class ApplicationNotFoundException extends BaseException {

    public ApplicationNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "APPLICATION_NOT_FOUND");
    }
}
