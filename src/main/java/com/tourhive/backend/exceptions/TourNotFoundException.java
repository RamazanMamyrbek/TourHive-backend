package com.tourhive.backend.exceptions;

import org.springframework.http.HttpStatus;

public class TourNotFoundException extends BaseException {

    public TourNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "TOUR_NOT_FOUND");
    }
}
