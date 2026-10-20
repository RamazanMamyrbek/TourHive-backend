package com.tourhive.backend.exceptions;

import org.springframework.http.HttpStatus;

public class ReviewAccessDeniedException extends BaseException {
    public ReviewAccessDeniedException(String message) {
        super(message, HttpStatus.FORBIDDEN, "REVIEW_ACCESS_DENIED");
    }
}
