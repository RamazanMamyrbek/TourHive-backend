package com.tourhive.backend.exceptions;

import org.springframework.http.HttpStatus;

public class CommercialTourNotAllowedException extends BaseException {

    public CommercialTourNotAllowedException(String message) {
        super(message, HttpStatus.FORBIDDEN, "COMMERCIAL_TOUR_NOT_ALLOWED");
    }
}
