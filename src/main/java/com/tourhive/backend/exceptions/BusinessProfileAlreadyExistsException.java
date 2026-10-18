package com.tourhive.backend.exceptions;

import org.springframework.http.HttpStatus;

public class BusinessProfileAlreadyExistsException extends BaseException {

    public BusinessProfileAlreadyExistsException(String message) {
        super(message, HttpStatus.CONFLICT, "BUSINESS_PROFILE_ALREADY_EXISTS");
    }
}
