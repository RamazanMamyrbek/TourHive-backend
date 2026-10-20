package com.tourhive.backend.exceptions;

import org.springframework.http.HttpStatus;

public class DuplicateReportException extends BaseException {
    public DuplicateReportException(String message) {
        super(message, HttpStatus.CONFLICT, "DUPLICATE_REPORT");
    }
}
