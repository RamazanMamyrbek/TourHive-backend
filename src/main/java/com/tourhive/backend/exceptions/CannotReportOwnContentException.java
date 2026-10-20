package com.tourhive.backend.exceptions;

import org.springframework.http.HttpStatus;

public class CannotReportOwnContentException extends BaseException {
    public CannotReportOwnContentException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "CANNOT_REPORT_OWN_CONTENT");
    }
}
