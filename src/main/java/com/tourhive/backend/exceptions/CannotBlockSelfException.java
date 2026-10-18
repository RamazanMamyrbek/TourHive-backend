package com.tourhive.backend.exceptions;

import org.springframework.http.HttpStatus;

public class CannotBlockSelfException extends BaseException {

    public CannotBlockSelfException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "CANNOT_BLOCK_SELF");
    }
}
