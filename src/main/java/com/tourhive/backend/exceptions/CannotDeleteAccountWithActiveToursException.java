package com.tourhive.backend.exceptions;

import org.springframework.http.HttpStatus;

public class CannotDeleteAccountWithActiveToursException extends BaseException {
    public CannotDeleteAccountWithActiveToursException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "CANNOT_DELETE_ACCOUNT_WITH_ACTIVE_TOURS");
    }
}
