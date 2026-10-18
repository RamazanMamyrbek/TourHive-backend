package com.tourhive.backend.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class BaseException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    protected BaseException(String message, HttpStatus status, String code) {
        super(message);
        this.status = status;
        this.code = code;
    }

    protected BaseException(String message, Throwable cause, HttpStatus status, String code) {
        super(message, cause);
        this.status = status;
        this.code = code;
    }
}
