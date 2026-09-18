package com.example.vehicalrentalserviceplatform.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<FieldValidationError> errors = new ArrayList<>();

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.add(new FieldValidationError(fieldError.getField(), fieldError.getDefaultMessage()));
        }

        ValidationErrorResponse response = new ValidationErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                LocalDateTime.now(),
                errors
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<SimpleErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        SimpleErrorResponse response = new SimpleErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<SimpleErrorResponse> handleBadRequestBody(HttpMessageNotReadableException ex) {
        SimpleErrorResponse response = new SimpleErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Request body is invalid or has the wrong format.",
                LocalDateTime.now()
        );

        return ResponseEntity.badRequest().body(response);
    }

    public static class ValidationErrorResponse {
        private final int status;
        private final String message;
        private final LocalDateTime timestamp;
        private final List<FieldValidationError> errors;

        public ValidationErrorResponse(int status, String message, LocalDateTime timestamp, List<FieldValidationError> errors) {
            this.status = status;
            this.message = message;
            this.timestamp = timestamp;
            this.errors = errors;
        }

        public int getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public List<FieldValidationError> getErrors() {
            return errors;
        }
    }

    public static class FieldValidationError {
        private final String field;
        private final String message;

        public FieldValidationError(String field, String message) {
            this.field = field;
            this.message = message;
        }

        public String getField() {
            return field;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class SimpleErrorResponse {
        private final int status;
        private final String message;
        private final LocalDateTime timestamp;

        public SimpleErrorResponse(int status, String message, LocalDateTime timestamp) {
            this.status = status;
            this.message = message;
            this.timestamp = timestamp;
        }

        public int getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }
}
