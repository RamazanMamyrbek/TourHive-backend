package com.tourhive.backend.exceptions;

import org.springframework.http.HttpStatus;

public class ReviewNotFoundException extends BaseException {
    public ReviewNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "REVIEW_NOT_FOUND");
    }
}
