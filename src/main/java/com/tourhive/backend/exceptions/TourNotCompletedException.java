package com.tourhive.backend.exceptions;

import org.springframework.http.HttpStatus;

public class TourNotCompletedException extends BaseException {
    public TourNotCompletedException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "TOUR_NOT_COMPLETED");
    }
}
