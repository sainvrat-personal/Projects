package com.example.orderservice.exception;

import com.example.orderservice.dto.ErrorResponse;
import com.example.orderservice.dto.ErrorResponse.ValidationError;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private HttpServletRequest mockRequest(String path) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(path);
        return request;
    }

    @Test
    void handleResourceNotFound_buildsNotFoundResponse() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ResourceNotFoundException ex = new ResourceNotFoundException("Order not found");
        HttpServletRequest request = mockRequest("/orders/123");

        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(404, body.getStatus());
        assertEquals("Not Found", body.getError());
        assertEquals("RESOURCE_NOT_FOUND", body.getCode());
        assertEquals("Order not found", body.getMessage());
        assertEquals("/orders/123", body.getPath());
        assertNotNull(body.getTimestamp());
        assertEquals("The requested resource does not exist", body.getDetails().get("reason"));
        assertNull(body.getValidationErrors());
    }

    @Test
    void handleIllegalState_buildsUnprocessableEntityResponse() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        IllegalStateException ex = new IllegalStateException("Invalid state transition");
        HttpServletRequest request = mockRequest("/orders/transition");

        ResponseEntity<ErrorResponse> response = handler.handleIllegalState(ex, request);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(422, body.getStatus());
        assertEquals("BUSINESS_RULE_VIOLATION", body.getCode());
        assertEquals("Invalid state transition", body.getMessage());
        assertEquals("/orders/transition", body.getPath());
        assertEquals(
                "The request failed a business rule or state machine constraint",
                body.getDetails().get("reason")
        );
    }

    @Test
    void handleBadRequest_buildsInvalidRequestResponse() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        IllegalArgumentException ex = new IllegalArgumentException("Bad argument");
        HttpServletRequest request = mockRequest("/path");

        ResponseEntity<ErrorResponse> response = handler.handleBadRequest(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(400, body.getStatus());
        assertEquals("INVALID_REQUEST", body.getCode());
        assertEquals("Bad argument", body.getMessage());
        assertEquals("/path", body.getPath());
        assertEquals(
                "The request contains invalid or malformed data",
                body.getDetails().get("reason")
        );
    }

    @Test
    void handleBadRequest_withTypeMismatchStillBuildsInvalidRequest() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MethodArgumentTypeMismatchException ex =
                new MethodArgumentTypeMismatchException("value", Integer.class, "param", null, new IllegalArgumentException("type mismatch"));
        HttpServletRequest request = mockRequest("/mismatch");

        ResponseEntity<ErrorResponse> response = handler.handleBadRequest(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("INVALID_REQUEST", body.getCode());
        assertEquals("/mismatch", body.getPath());
    }

    @Test
    void handleValidation_buildsValidationErrorsList() throws Exception {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest request = mockRequest("/validate");

        Object target = new Object();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "request");
        bindingResult.addError(new FieldError("request", "field1", "must not be null"));
        bindingResult.addError(new FieldError("request", "field2", "must be positive"));

        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("VALIDATION_FAILED", body.getCode());
        assertEquals("Request validation failed", body.getMessage());
        assertEquals("/validate", body.getPath());

        List<ValidationError> errors = body.getValidationErrors();
        assertNotNull(errors);
        assertEquals(2, errors.size());
        assertTrue(errors.stream().anyMatch(e -> e.getField().equals("field1") && e.getMessage().equals("must not be null")));
        assertTrue(errors.stream().anyMatch(e -> e.getField().equals("field2") && e.getMessage().equals("must be positive")));
    }

    @SuppressWarnings("deprecation")
    @Test
    void handleUnreadableMessage_usesMostSpecificCauseWhenPresent() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest request = mockRequest("/unreadable");

        RuntimeException cause = new RuntimeException("inner cause");
        HttpMessageNotReadableException ex =
                new HttpMessageNotReadableException("wrapper", cause);

        ResponseEntity<ErrorResponse> response = handler.handleUnreadableMessage(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("INVALID_REQUEST", body.getCode());
        assertEquals("inner cause", body.getMessage());
        assertEquals("/unreadable", body.getPath());
        assertEquals("The request body is missing or malformed", body.getDetails().get("reason"));
    }

    @SuppressWarnings("deprecation")
    @Test
    void handleUnreadableMessage_fallsBackToTopLevelMessageWhenNoSpecificCause() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest request = mockRequest("/unreadable2");

        HttpMessageNotReadableException ex =
                new HttpMessageNotReadableException("no specific cause");

        ResponseEntity<ErrorResponse> response = handler.handleUnreadableMessage(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("no specific cause", body.getMessage());
        assertEquals("/unreadable2", body.getPath());
    }

    @Test
    void handleConcurrency_withOptimisticLockingFailureBuildsConflictResponse() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest request = mockRequest("/concurrent");

        ObjectOptimisticLockingFailureException ex =
                new ObjectOptimisticLockingFailureException("Order", UUID.randomUUID());

        ResponseEntity<ErrorResponse> response = handler.handleConcurrency(ex, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("CONCURRENT_MODIFICATION", body.getCode());
        assertEquals("The resource was modified by another request. Please retry.", body.getMessage());
        assertEquals("/concurrent", body.getPath());
        assertNotNull(body.getDetails().get("technicalMessage"));
    }

    @Test
    void handleConcurrency_withGenericConcurrencyFailureBuildsConflictResponse() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest request = mockRequest("/concurrent2");

        ConcurrencyFailureException ex = new ConcurrencyFailureException("row locked");

        ResponseEntity<ErrorResponse> response = handler.handleConcurrency(ex, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("CONCURRENT_MODIFICATION", body.getCode());
        assertEquals("/concurrent2", body.getPath());
        assertEquals("row locked", body.getDetails().get("technicalMessage"));
    }

    @Test
    void handleGeneric_buildsInternalServerErrorResponse() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest request = mockRequest("/generic");

        Exception ex = new Exception("boom");

        ResponseEntity<ErrorResponse> response = handler.handleGeneric(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("SERVER_ERROR", body.getCode());
        assertEquals("An unexpected error occurred", body.getMessage());
        assertEquals("/generic", body.getPath());
        assertEquals("boom", body.getDetails().get("technicalMessage"));
    }

    @Test
    void buildErrorResponse_handlesNullRequestPathViaReflection() throws Exception {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        Method method = GlobalExceptionHandler.class.getDeclaredMethod(
                "buildErrorResponse",
                HttpStatus.class,
                String.class,
                String.class,
                HttpServletRequest.class,
                Map.class,
                List.class
        );
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        ResponseEntity<ErrorResponse> response = (ResponseEntity<ErrorResponse>) method.invoke(
                handler,
                HttpStatus.BAD_REQUEST,
                "CODE",
                "msg",
                null,
                null,
                null
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertNull(body.getPath(), "Path should be null when request is null");
        assertEquals("CODE", body.getCode());
        assertEquals("msg", body.getMessage());
    }
}

