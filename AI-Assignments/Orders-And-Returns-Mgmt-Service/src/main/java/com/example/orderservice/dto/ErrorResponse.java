package com.example.orderservice.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {

    /**
     * HTTP status code (e.g. 400, 404).
     */
    private int status;

    /**
     * HTTP reason phrase or short error title (e.g. "Bad Request").
     */
    private String error;

    /**
     * Stable, application-specific error code (e.g. ORDER_NOT_FOUND).
     */
    private String code;

    /**
     * Human-readable error message.
     */
    private String message;

    /**
     * Request path that generated the error.
     */
    private String path;

    /**
     * Timestamp when the error occurred.
     */
    private Instant timestamp;

    /**
     * Optional structured details for machine consumers.
     * For validation errors, this can contain field-to-message mappings or
     * other key/value metadata.
     */
    private Map<String, String> details;

    /**
     * Optional list of per-field validation errors.
     * Present primarily for 400 Bad Request scenarios.
     */
    private List<ValidationError> validationErrors;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ValidationError {
        private String field;
        private String message;
    }
}