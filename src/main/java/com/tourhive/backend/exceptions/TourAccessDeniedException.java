package com.tourhive.backend.exceptions;

import org.springframework.http.HttpStatus;

public class TourAccessDeniedException extends BaseException {

    public TourAccessDeniedException(String message) {
        super(message, HttpStatus.FORBIDDEN, "FORBIDDEN_TOUR_ACCESS");
    }
}
