package com.example.orderservice.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ErrorResponseTest {

    @Test
    void builder_populatesAllFieldsCorrectly() {
        Instant now = Instant.now();

        ErrorResponse error = ErrorResponse.builder()
                .status(400)
                .error("Bad Request")
                .code("INVALID_REQUEST")
                .message("Invalid input data")
                .path("/orders/order")
                .timestamp(now)
                .details(Collections.singletonMap("reason", "The request contains invalid or malformed data"))
                .validationErrors(null)
                .build();

        assertEquals(400, error.getStatus());
        assertEquals("Bad Request", error.getError());
        assertEquals("INVALID_REQUEST", error.getCode());
        assertEquals("Invalid input data", error.getMessage());
        assertEquals("/orders/order", error.getPath());
        assertEquals(now, error.getTimestamp());
        assertNotNull(error.getDetails());
        assertEquals("The request contains invalid or malformed data", error.getDetails().get("reason"));
        assertNull(error.getValidationErrors());
    }

    @Test
    void noArgsConstructorAndSetters_workAsExpected() {
        Instant now = Instant.now();

        ErrorResponse error = new ErrorResponse();
        error.setStatus(404);
        error.setError("Not Found");
        error.setCode("RESOURCE_NOT_FOUND");
        error.setMessage("Order not found");
        error.setPath("/orders/123");
        error.setTimestamp(now);
        error.setDetails(Collections.singletonMap("reason", "The requested resource does not exist"));

        ErrorResponse.ValidationError ve = new ErrorResponse.ValidationError();
        ve.setField("orderId");
        ve.setMessage("must not be null");
        error.setValidationErrors(Collections.singletonList(ve));

        assertEquals(404, error.getStatus());
        assertEquals("Not Found", error.getError());
        assertEquals("RESOURCE_NOT_FOUND", error.getCode());
        assertEquals("Order not found", error.getMessage());
        assertEquals("/orders/123", error.getPath());
        assertEquals(now, error.getTimestamp());
        assertEquals("The requested resource does not exist", error.getDetails().get("reason"));
        assertEquals(1, error.getValidationErrors().size());
        assertEquals("orderId", error.getValidationErrors().get(0).getField());
        assertEquals("must not be null", error.getValidationErrors().get(0).getMessage());
    }

    @Test
    void validationError_builderAndEqualsHashCode() {
        ErrorResponse.ValidationError v1 = ErrorResponse.ValidationError.builder()
                .field("email")
                .message("must be a well-formed email address")
                .build();

        ErrorResponse.ValidationError v2 = ErrorResponse.ValidationError.builder()
                .field("email")
                .message("must be a well-formed email address")
                .build();

        ErrorResponse.ValidationError v3 = ErrorResponse.ValidationError.builder()
                .field("email")
                .message("is required")
                .build();

        assertEquals(v1, v2);
        assertEquals(v1.hashCode(), v2.hashCode());
        assertNotEquals(v1, v3);
    }

    @Test
    void errorResponse_equalsAndHashCodeBasedOnFields() {
        Instant now = Instant.now();
        Map<String, String> details = Collections.singletonMap("reason", "something went wrong");

        ErrorResponse e1 = ErrorResponse.builder()
                .status(500)
                .error("Internal Server Error")
                .code("SERVER_ERROR")
                .message("An unexpected error occurred")
                .path("/any/path")
                .timestamp(now)
                .details(details)
                .validationErrors(Collections.emptyList())
                .build();

        ErrorResponse e2 = ErrorResponse.builder()
                .status(500)
                .error("Internal Server Error")
                .code("SERVER_ERROR")
                .message("An unexpected error occurred")
                .path("/any/path")
                .timestamp(now)
                .details(details)
                .validationErrors(Collections.emptyList())
                .build();

        ErrorResponse e3 = ErrorResponse.builder()
                .status(400)
                .error("Bad Request")
                .code("INVALID_REQUEST")
                .message("Bad input")
                .path("/other/path")
                .timestamp(now)
                .details(Collections.emptyMap())
                .validationErrors(null)
                .build();

        assertEquals(e1, e2);
        assertEquals(e1.hashCode(), e2.hashCode());
        assertNotEquals(e1, e3);
    }

    @Test
    void toString_doesNotThrowAndContainsKeyFields() {
        ErrorResponse error = ErrorResponse.builder()
                .status(400)
                .error("Bad Request")
                .code("INVALID_REQUEST")
                .message("Something went wrong")
                .path("/path")
                .build();

        String s = error.toString();
        assertNotNull(s);
        assertTrue(s.contains("INVALID_REQUEST"));
        assertTrue(s.contains("Something went wrong"));
    }
}

