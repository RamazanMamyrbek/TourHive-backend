package com.tourhive.backend.exceptions;

import org.springframework.http.HttpStatus;

public class ReportNotFoundException extends BaseException {
    public ReportNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "REPORT_NOT_FOUND");
    }
}
