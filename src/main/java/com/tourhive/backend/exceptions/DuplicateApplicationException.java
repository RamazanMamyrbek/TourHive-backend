package com.tourhive.backend.exceptions;

import org.springframework.http.HttpStatus;

public class DuplicateApplicationException extends BaseException {

    public DuplicateApplicationException(String message) {
        super(message, HttpStatus.CONFLICT, "DUPLICATE_APPLICATION");
    }
}
