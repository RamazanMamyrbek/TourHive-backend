package com.tourhive.backend.exceptions;

import org.springframework.http.HttpStatus;

public class DuplicateReviewException extends BaseException {
    public DuplicateReviewException(String message) {
        super(message, HttpStatus.CONFLICT, "DUPLICATE_REVIEW");
    }
}
