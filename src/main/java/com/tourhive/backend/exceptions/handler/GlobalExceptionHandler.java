package com.tourhive.backend.exceptions.handler;

import com.tourhive.backend.dto.errors.ApiErrorResponseDto;
import com.tourhive.backend.dto.errors.ApiFieldErrorDto;
import com.tourhive.backend.exceptions.BaseException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiErrorResponseDto> handleBaseException(BaseException ex, HttpServletRequest request) {
        log.warn("Domain exception [{}]: {}", ex.getCode(), ex.getMessage());
        ApiErrorResponseDto error = ApiErrorResponseDto.of(
                ex.getStatus().value(),
                ex.getCode(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(ex.getStatus()).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponseDto> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiFieldErrorDto> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::mapFieldError)
                .toList();

        ApiErrorResponseDto error = ApiErrorResponseDto.of(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_FAILED",
                "Validation failed for one or more fields",
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponseDto> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        List<ApiFieldErrorDto> fieldErrors = ex.getConstraintViolations().stream()
                .map(cv -> new ApiFieldErrorDto(
                        cv.getPropertyPath().toString(),
                        cv.getMessage(),
                        cv.getInvalidValue()
                ))
                .toList();

        ApiErrorResponseDto error = ApiErrorResponseDto.of(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_FAILED",
                "Constraint violation occurred",
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponseDto> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied on {}: {}", request.getRequestURI(), ex.getMessage());
        ApiErrorResponseDto error = ApiErrorResponseDto.of(
                HttpStatus.FORBIDDEN.value(),
                "FORBIDDEN",
                "Access is denied",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponseDto> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        log.warn("Authentication failed on {}: {}", request.getRequestURI(), ex.getMessage());
        ApiErrorResponseDto error = ApiErrorResponseDto.of(
                HttpStatus.UNAUTHORIZED.value(),
                "UNAUTHORIZED",
                "Authentication required or credentials invalid",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponseDto> handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
        ApiErrorResponseDto error = ApiErrorResponseDto.of(
                HttpStatus.NOT_FOUND.value(),
                "RESOURCE_NOT_FOUND",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(org.springframework.data.mapping.PropertyReferenceException.class)
    public ResponseEntity<ApiErrorResponseDto> handlePropertyReferenceException(org.springframework.data.mapping.PropertyReferenceException ex, HttpServletRequest request) {
        log.warn("Invalid sort property on {}: {}", request.getRequestURI(), ex.getMessage());
        ApiErrorResponseDto error = ApiErrorResponseDto.of(
                HttpStatus.BAD_REQUEST.value(),
                "INVALID_SORT_PROPERTY",
                "Invalid sort property: " + ex.getPropertyName(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponseDto> handleMaxUploadSizeExceeded(org.springframework.web.multipart.MaxUploadSizeExceededException ex, HttpServletRequest request) {
        log.warn("Max upload size exceeded on {}: {}", request.getRequestURI(), ex.getMessage());
        ApiErrorResponseDto error = ApiErrorResponseDto.of(
                HttpStatus.BAD_REQUEST.value(),
                "FILE_SIZE_EXCEEDED",
                "Uploaded file exceeds maximum allowed size",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponseDto> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled server error processing request to {}", request.getRequestURI(), ex);
        ApiErrorResponseDto error = ApiErrorResponseDto.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_SERVER_ERROR",
                "An unexpected internal error occurred",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    private ApiFieldErrorDto mapFieldError(FieldError error) {
        return new ApiFieldErrorDto(
                error.getField(),
                error.getDefaultMessage(),
                error.getRejectedValue()
        );
    }
}
