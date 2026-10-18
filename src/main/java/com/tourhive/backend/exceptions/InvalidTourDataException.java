package com.tourhive.backend.exceptions;

import org.springframework.http.HttpStatus;

public class InvalidTourDataException extends BaseException {

    public InvalidTourDataException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_TOUR_DATA");
    }
}
