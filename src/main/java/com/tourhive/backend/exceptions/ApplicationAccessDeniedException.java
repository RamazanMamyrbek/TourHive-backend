package com.tourhive.backend.exceptions;

import org.springframework.http.HttpStatus;

public class ApplicationAccessDeniedException extends BaseException {

    public ApplicationAccessDeniedException(String message) {
        super(message, HttpStatus.FORBIDDEN, "FORBIDDEN_APPLICATION_ACCESS");
    }
}
