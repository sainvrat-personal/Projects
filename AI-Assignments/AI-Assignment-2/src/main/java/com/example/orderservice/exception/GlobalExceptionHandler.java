package com.example.orderservice.exception;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import com.example.orderservice.dto.ErrorResponse;
import com.example.orderservice.dto.ErrorResponse.ValidationError;

import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Global exception handler that ensures all API endpoints return a
 * standardized error response structure.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

        private ResponseEntity<ErrorResponse> buildErrorResponse(
                        HttpStatus status,
                        String code,
                        String message,
                        HttpServletRequest request,
                        Map<String, String> details,
                        List<ValidationError> validationErrors) {

                ErrorResponse body = ErrorResponse.builder()
                                .status(status.value())
                                .error(status.getReasonPhrase())
                                .code(code)
                                .message(message)
                                .path(request != null ? request.getRequestURI() : null)
                                .timestamp(Instant.now())
                                .details(details)
                                .validationErrors(validationErrors)
                                .build();

                return ResponseEntity.status(status).body(body);
        }

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex,
                        HttpServletRequest request) {
                return buildErrorResponse(
                                HttpStatus.NOT_FOUND,
                                "RESOURCE_NOT_FOUND",
                                ex.getMessage(),
                                request,
                                Collections.singletonMap("reason", "The requested resource does not exist"),
                                null);
        }

        @ExceptionHandler(IllegalStateException.class)
        public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
                // Used for invalid state transitions or business rule violations
                return buildErrorResponse(
                                HttpStatus.UNPROCESSABLE_ENTITY,
                                "BUSINESS_RULE_VIOLATION",
                                ex.getMessage(),
                                request,
                                Collections.singletonMap("reason",
                                                "The request failed a business rule or state machine constraint"),
                                null);
        }

        @ExceptionHandler({
                        IllegalArgumentException.class,
                        MethodArgumentTypeMismatchException.class
        })
        public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException ex, HttpServletRequest request) {
                return buildErrorResponse(
                                HttpStatus.BAD_REQUEST,
                                "INVALID_REQUEST",
                                ex.getMessage(),
                                request,
                                Collections.singletonMap("reason", "The request contains invalid or malformed data"),
                                null);
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                        HttpServletRequest request) {

                List<ValidationError> fieldErrors = ex.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .map(err -> ValidationError.builder()
                                                .field(err.getField())
                                                .message(err.getDefaultMessage())
                                                .build())
                                .collect(Collectors.toList());

                return buildErrorResponse(
                                HttpStatus.BAD_REQUEST,
                                "VALIDATION_FAILED",
                                "Request validation failed",
                                request,
                                null,
                                fieldErrors);
        }

        /**
         * Handle cases where the HTTP request body cannot be read or parsed
         * (malformed JSON, missing body where one is required, etc.).
         */
        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<ErrorResponse> handleUnreadableMessage(HttpMessageNotReadableException ex,
                        HttpServletRequest request) {
                return buildErrorResponse(
                                HttpStatus.BAD_REQUEST,
                                "INVALID_REQUEST",
                                ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage()
                                                : ex.getMessage(),
                                request,
                                Collections.singletonMap("reason", "The request body is missing or malformed"),
                                null);
        }

        @ExceptionHandler({
                        ObjectOptimisticLockingFailureException.class,
                        ConcurrencyFailureException.class
        })
        public ResponseEntity<ErrorResponse> handleConcurrency(Exception ex, HttpServletRequest request) {
                return buildErrorResponse(
                                HttpStatus.CONFLICT,
                                "CONCURRENT_MODIFICATION",
                                "The resource was modified by another request. Please retry.",
                                request,
                                Collections.singletonMap("technicalMessage", ex.getMessage()),
                                null);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
                return buildErrorResponse(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "SERVER_ERROR",
                                "An unexpected error occurred",
                                request,
                                Collections.singletonMap("technicalMessage", ex.getMessage()),
                                null);
        }
}
