package com.tourhive.backend.exceptions;

import org.springframework.http.HttpStatus;

public class TourCapacityExceededException extends BaseException {

    public TourCapacityExceededException(String message) {
        super(message, HttpStatus.CONFLICT, "TOUR_CAPACITY_EXCEEDED");
    }
}
