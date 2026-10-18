package com.tourhive.backend.exceptions;

import org.springframework.http.HttpStatus;

public class InvalidFileException extends BaseException {

    public InvalidFileException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_FILE");
    }
}
