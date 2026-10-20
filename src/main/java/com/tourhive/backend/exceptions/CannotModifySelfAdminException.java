package com.tourhive.backend.exceptions;

import org.springframework.http.HttpStatus;

public class CannotModifySelfAdminException extends BaseException {
    public CannotModifySelfAdminException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "CANNOT_MODIFY_SELF_ADMIN");
    }
}
