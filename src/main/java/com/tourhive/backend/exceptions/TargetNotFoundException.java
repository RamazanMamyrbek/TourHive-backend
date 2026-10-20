package com.tourhive.backend.exceptions;

import org.springframework.http.HttpStatus;

public class TargetNotFoundException extends BaseException {
    public TargetNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }
}
