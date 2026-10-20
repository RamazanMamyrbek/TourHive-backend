package com.tourhive.backend.exceptions;

import org.springframework.http.HttpStatus;

public class CannotReviewOwnTourException extends BaseException {
    public CannotReviewOwnTourException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "CANNOT_REVIEW_OWN_TOUR");
    }
}
