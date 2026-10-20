package com.tourhive.backend.exceptions;

import org.springframework.http.HttpStatus;

public class TourCancelledException extends BaseException {
    public TourCancelledException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "TOUR_CANCELLED");
    }
}
