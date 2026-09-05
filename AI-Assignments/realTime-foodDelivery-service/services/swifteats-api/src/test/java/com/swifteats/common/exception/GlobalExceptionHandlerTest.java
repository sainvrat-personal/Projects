package com.swifteats.common.exception;

import com.swifteats.common.exception.AuthenticationRequiredException;
import com.swifteats.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleImportInProgress_returns409() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/admin/analytics/import");

        ResponseEntity<ErrorResponse> response = handler.handleImportInProgress(
                new ImportInProgressException(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("IMPORT_IN_PROGRESS");
        assertThat(response.getBody().path()).isEqualTo("/api/v1/admin/analytics/import");
    }

    @Test
    void handleAuthenticationRequired_returns401() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/orders");

        ResponseEntity<ErrorResponse> response = handler.handleAuthenticationRequired(
                new AuthenticationRequiredException("Authenticated customer context is required"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().code()).isEqualTo("UNAUTHORIZED");
    }

    @Test
    void handleAccessDenied_returns403() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/orders/x");

        ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(
                new AccessDeniedException("Order access denied"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().code()).isEqualTo("ACCESS_DENIED");
    }

    @Test
    void handleNotFound_returns404() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/restaurants/x/menu");

        ResponseEntity<ErrorResponse> response = handler.handleNotFound(
                new ResourceNotFoundException("Restaurant not found"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(response.getBody().code()).isEqualTo("NOT_FOUND");
    }

    @Test
    void handleNotFound_returnsJsonForSsePaths() {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/api/v1/orders/x/tracking/stream");
        request.addHeader("Accept", MediaType.TEXT_EVENT_STREAM_VALUE);

        ResponseEntity<ErrorResponse> response = handler.handleNotFound(
                new ResourceNotFoundException("Driver not assigned to this order yet"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    }

    @Test
    void handleIllegalArgument_returns400() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/test");

        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(
                new IllegalArgumentException("bad input"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo("INVALID_REQUEST");
    }

    @Test
    void handleIllegalState_returns422() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/admin/analytics/import");

        ResponseEntity<ErrorResponse> response = handler.handleIllegalState(
                new IllegalStateException("Failed to read CSV"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().code()).isEqualTo("IMPORT_FAILED");
    }
}
