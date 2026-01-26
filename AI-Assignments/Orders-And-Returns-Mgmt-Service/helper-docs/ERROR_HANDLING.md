# Error Handling Guide

This document describes the standardized error handling approach used throughout the Order & Returns Management System.

## Error Response Structure

All API endpoints return consistent error responses with the following structure:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Detailed error message",
  "timestamp": "2023-07-01T12:34:56.789Z",
  "path": "/api/endpoint/path",
  "details": ["Additional error detail 1", "Additional error detail 2"]
}
```

### Fields Explanation

- `status`: HTTP status code
- `error`: Standard HTTP status reason phrase
- `message`: Human-readable error message
- `timestamp`: ISO-8601 formatted timestamp when the error occurred
- `path`: The API endpoint path that generated the error
- `details`: Optional array of detailed error messages (for validation errors, etc.)

## Common HTTP Status Codes

| Status Code | Description | Common Use Cases |
|-------------|-------------|-----------------|
| 400 | Bad Request | Invalid input, missing required fields, malformed request body |
| 401 | Unauthorized | Authentication required but not provided |
| 403 | Forbidden | Authentication provided but lacks permission |
| 404 | Not Found | Resource not found (invalid ID, etc.) |
| 409 | Conflict | Resource conflict (e.g., invalid state transition) |
| 422 | Unprocessable Entity | Request validated but failed business logic |
| 500 | Internal Server Error | Unexpected server error |

## Error Examples

### Validation Error (400)

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for request",
  "timestamp": "2023-07-01T12:34:56.789Z",
  "path": "/orders",
  "details": [
    "customerId: must not be null",
    "items: size must be between 1 and 100"
  ]
}
```

### Resource Not Found (404)

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Order with ID 12345 not found",
  "timestamp": "2023-07-01T12:34:56.789Z",
  "path": "/orders/12345",
  "details": []
}
```

### Business Rule Violation (422)

```json
{
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Only orders in PENDING_PAYMENT or PAID state can be cancelled",
  "timestamp": "2023-07-01T12:34:56.789Z",
  "path": "/orders/123/cancel",
  "code": "BUSINESS_RULE_VIOLATION",
  "details": {
    "reason": "The request failed a business rule or state machine constraint"
  }
}
```

### Malformed Request or Type Mismatch (400)

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Failed to convert value of type 'java.lang.String' to required type 'java.util.UUID'; nested exception is ...",
  "timestamp": "2023-07-01T12:34:56.789Z",
  "path": "/orders/not-a-uuid/state-history",
  "code": "INVALID_REQUEST",
  "details": {
    "reason": "The request contains invalid or malformed data"
  }
}
```

### Server Error (500)

```json
{
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred",
  "timestamp": "2023-07-01T12:34:56.789Z",
  "path": "/orders/123/invoice",
  "details": []
}
```

## Exception Handling Implementation

The system uses Spring's `@ControllerAdvice` to handle exceptions globally:

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            HttpStatus.NOT_FOUND.getReasonPhrase(),
            ex.getMessage(),
            new Date(),
            ((ServletWebRequest) request).getRequest().getRequestURI(),
            Collections.emptyList()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStateTransition(InvalidStateTransitionException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.CONFLICT.value(),
            HttpStatus.CONFLICT.getReasonPhrase(),
            "Invalid state transition",
            new Date(),
            ((ServletWebRequest) request).getRequest().getRequestURI(),
            Collections.singletonList(ex.getMessage())
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    // Additional exception handlers...
}
```

## Client Error Handling

Clients consuming this API should:

1. Check the HTTP status code first
2. Parse the error response body
3. Present appropriate feedback to users based on the error details
4. Log errors for troubleshooting

Example client-side error handling:

```javascript
fetch('/api/orders/123')
  .then(response => {
    if (!response.ok) {
      return response.json().then(errorData => {
        throw errorData;
      });
    }
    return response.json();
  })
  .then(data => {
    // Handle successful response
  })
  .catch(error => {
    // Handle error response
    console.error(`Error (${error.status}): ${error.message}`);
    // Display error to user based on status code and message
  });
```