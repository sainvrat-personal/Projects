# API Endpoints Documentation

This document provides detailed information about all API endpoints in the Order & Returns Management System.

## Base URL
- Local Development: `http://localhost:8080`
- API Documentation (Swagger): `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`

## Order Management APIs

### 1. Create Order
**Endpoint:** `POST /orders/order`

**Description:** Creates a new order with initial status `PENDING_PAYMENT`

**Request Body:**
```json
{
  "customerId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "item": {
    "productId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "quantity": 1,
    "price": 99.99
  },
  "shippingAddress": "string",
  "email": "customer@gmail.com",
  "transactionId": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
}
```

**Response (200 OK):**
```json
{
  "orderId": "uuid",
  "status": "PENDING_PAYMENT",
  "createdAt": "2025-09-21T10:00:00Z"
}
```

**Error Responses:**
- `400 Bad Request` - Invalid request data
- `500 Internal Server Error` - Server error

### 2. Update Order State
**Endpoint:** `PATCH /orders/{orderId}/state`

**Description:** Transitions order to a new state

**Request Body:**
```json
{
  "newState": "PAID",
  "changedBy": "uuid"
}
```

**Response (200 OK):**
```json
{
  "id": "uuid",
  "transactionId": "uuid",
  "customerId": "uuid",
  "status": "PAID",
  "shippingAddress": "string",
  "email": "customer@gmail.com",
  "productId": "uuid",
  "quantity": 1,
  "price": 99.99,
  "createdAt": "2025-09-21T10:00:00Z",
  "updatedAt": "2025-09-21T10:05:00Z"
}
```

**Error Responses:**
- `404 Not Found` - Order not found
- `422 Unprocessable Entity` - Invalid state transition
- `500 Internal Server Error` - Server error

### 3. Cancel Order
**Endpoint:** `PATCH /orders/{orderId}/cancel`

**Description:** Cancels an order (only allowed for `PENDING_PAYMENT` or `PAID` states)

**Request Body:**
```json
{
  "reason": "Customer requested cancellation",
  "changedBy": "uuid"
}
```

**Response (200 OK):**
```json
{
  "id": "uuid",
  "transactionId": "uuid",
  "customerId": "uuid",
  "status": "CANCELLED",
  "failureReason": "Customer requested cancellation",
  "shippingAddress": "string",
  "email": "customer@gmail.com",
  "productId": "uuid",
  "quantity": 1,
  "price": 99.99,
  "createdAt": "2025-09-21T10:00:00Z",
  "updatedAt": "2025-09-21T10:05:00Z"
}
```

**Error Responses:**
- `404 Not Found` - Order not found
- `422 Unprocessable Entity` - Invalid state for cancellation
- `500 Internal Server Error` - Server error

### 4. Get Order State History
**Endpoint:** `GET /orders/{orderId}/state-history`

**Description:** Retrieves complete state transition history for an order

**Response (200 OK):**
```json
[
  {
    "id": "uuid",
    "transactionId": "uuid",
    "entityType": "Order",
    "entityId": "uuid",
    "state": "PENDING_PAYMENT",
    "changedBy": "uuid",
    "timestamp": "2025-09-21T10:00:00Z",
    "createdAt": "2025-09-21T10:00:00Z",
    "updatedAt": "2025-09-21T10:00:00Z",
    "notes": null
  },
  {
    "id": "uuid",
    "transactionId": "uuid",
    "entityType": "Order",
    "entityId": "uuid",
    "state": "PAID",
    "changedBy": "uuid",
    "timestamp": "2025-09-21T10:05:00Z",
    "createdAt": "2025-09-21T10:05:00Z",
    "updatedAt": "2025-09-21T10:05:00Z",
    "notes": null
  }
]
```

**Error Responses:**
- `404 Not Found` - Order not found
- `500 Internal Server Error` - Server error

## Return Management APIs

### 1. Initiate Return
**Endpoint:** `POST /returns/{orderId}`

**Description:** Initiates a return for an order (only allowed for orders with status `DELIVERED`)

**Request Body:**
```json
{
  "changedBy": "uuid"
}
```

**Response (201 Created):**
```json
{
  "id": "uuid",
  "orderId": "uuid",
  "status": "REQUESTED",
  "createdAt": "2025-09-21T12:00:00Z"
}
```

**Error Responses:**
- `404 Not Found` - Order not found
- `422 Unprocessable Entity` - Return not allowed (order not delivered)
- `500 Internal Server Error` - Server error

### 2. Update Return State
**Endpoint:** `PATCH /returns/{returnId}/state`

**Description:** Transitions return to a new state

**Request Body:**
```json
{
  "newState": "APPROVED",
  "changedBy": "uuid"
}
```

**Response (200 OK):**
```json
{
  "id": "uuid",
  "orderId": "uuid",
  "status": "APPROVED",
  "createdAt": "2025-09-21T12:00:00Z"
}
```

**Error Responses:**
- `404 Not Found` - Return not found
- `422 Unprocessable Entity` - Invalid state transition
- `500 Internal Server Error` - Server error

### 3. Get Return State History
**Endpoint:** `GET /returns/{returnId}/state-history`

**Description:** Retrieves complete state transition history for a return

**Response (200 OK):**
```json
[
  {
    "id": "uuid",
    "transactionId": "uuid",
    "entityType": "Return",
    "entityId": "uuid",
    "state": "REQUESTED",
    "changedBy": "uuid",
    "timestamp": "2025-09-21T12:00:00Z",
    "createdAt": "2025-09-21T12:00:00Z",
    "updatedAt": "2025-09-21T12:00:00Z",
    "notes": null
  }
]
```

**Error Responses:**
- `404 Not Found` - Return not found
- `500 Internal Server Error` - Server error

## Job Management APIs

### 1. Get All Jobs
**Endpoint:** `GET /jobs`

**Description:** Retrieves all background jobs

**Response (200 OK):**
```json
[
  {
    "id": "uuid",
    "jobType": "REFUND_PROCESSING",
    "entityId": "uuid",
    "status": "COMPLETED",
    "retryCount": 0,
    "maxRetries": 3,
    "lastAttempt": "2025-09-21T12:30:00Z",
    "nextAttempt": null,
    "errorMessage": null,
    "result": "Refund processed successfully",
    "createdAt": "2025-09-21T12:30:00Z",
    "updatedAt": "2025-09-21T12:30:00Z",
    "idempotencyKey": "refund_uuid"
  }
]
```

### 2. Get Job by ID
**Endpoint:** `GET /jobs/{jobId}`

**Description:** Retrieves details of a specific job

**Response (200 OK):**
```json
{
  "id": "uuid",
  "jobType": "REFUND_PROCESSING",
  "entityId": "uuid",
  "status": "COMPLETED",
  "retryCount": 0,
  "maxRetries": 3,
  "lastAttempt": "2025-09-21T12:30:00Z",
  "nextAttempt": null,
  "errorMessage": null,
  "result": "Refund processed successfully",
  "createdAt": "2025-09-21T12:30:00Z",
  "updatedAt": "2025-09-21T12:30:00Z",
  "idempotencyKey": "refund_uuid"
}
```

**Error Responses:**
- `404 Not Found` - Job not found
- `500 Internal Server Error` - Server error

### 3. Get Jobs by Status
**Endpoint:** `GET /jobs/status/{status}`

**Description:** Retrieves jobs filtered by status

**Path Parameters:**
- `status`: One of `PENDING`, `IN_PROGRESS`, `COMPLETED`, `FAILED`, `RETRY`

**Response (200 OK):**
Array of `JobExecution` objects (see example above).

### 4. Get Jobs by Type
**Endpoint:** `GET /jobs/type/{type}`

**Description:** Retrieves jobs filtered by type.

**Path Parameters:**
- `type`: One of `INVOICE_GENERATION`, `REFUND_PROCESSING`

**Response (200 OK):**
Array of `JobExecution` objects.

### 5. Get Jobs by Type and Status
**Endpoint:** `GET /jobs/type/{type}/status/{status}`

**Description:** Retrieves jobs filtered by both type and status.

**Path Parameters:**
- `type`: One of `INVOICE_GENERATION`, `REFUND_PROCESSING`
- `status`: One of `PENDING`, `IN_PROGRESS`, `COMPLETED`, `FAILED`, `RETRY`

**Response (200 OK):**
Array of `JobExecution` objects.

### 6. Get Jobs for an Entity
**Endpoint:** `GET /jobs/entity/{entityId}`

**Description:** Retrieves all jobs associated with a specific business entity (order or return).

**Path Parameters:**
- `entityId`: UUID of the order or return.

**Response (200 OK):**
Array of `JobExecution` objects.

### 7. Trigger Job Processing Once
**Endpoint:** `POST /jobs/process`

**Description:** Manually triggers processing of pending and retryable invoice and refund jobs once. Useful for tests or when schedulers are temporarily disabled.

**Response (200 OK):**
```json
{
  "status": "OK",
  "message": "Job processing triggered successfully."
}
```

### 8. Mark Job for Retry
**Endpoint:** `POST /jobs/{jobId}/retry`

**Description:** Marks an existing job (typically `FAILED`, but may also be `COMPLETED`) as eligible for retry by setting its status to `RETRY` and `nextAttempt` to "now". The same `JobExecution` record is reused to preserve idempotency.

**Response (200 OK):**
```json
{
  "status": "OK",
  "message": "Job marked for retry successfully.",
  "jobId": "uuid",
  "jobStatus": "RETRY"
}
```

**Error Responses:**
- `404 Not Found` - Job not found
- `500 Internal Server Error` - Server error

## Mock Payment Service APIs

### 1. Process Refund
**Endpoint:** `POST /mock-payment/refund`

**Description:** Mock payment service endpoint for processing refunds

**Request Body:**
```json
{
  "returnId": "string",
  "amount": 99.99,
  "transactionId": "uuid"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "status": "COMPLETED",
  "message": "Refund of $99.99 for return returnId processed successfully",
  "refundId": "REF-12345678",
  "amount": 99.99,
  "transactionId": "uuid",
  "timestamp": "2025-09-21T13:00:00Z"
}
```

**Response (400 Bad Request) - Simulated Failure:**
```json
{
  "success": false,
  "status": "FAILED",
  "message": "Insufficient funds or account issue",
  "refundId": null,
  "amount": 99.99,
  "transactionId": "uuid",
  "timestamp": "2025-09-21T13:00:00Z"
}
```

## Status Codes and Error Handling

### Standard HTTP Status Codes
- `200 OK` - Successful GET, PATCH requests
- `201 Created` - Successful POST requests
- `400 Bad Request` - Invalid request data
- `404 Not Found` - Resource not found
- `422 Unprocessable Entity` - Invalid state transitions or business logic violations
- `500 Internal Server Error` - Server errors

### Error Response Format
All error responses follow this format:
```json
{
  "timestamp": "2025-09-21T10:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Order with ID uuid not found",
  "path": "/orders/uuid"
}
```

## Authentication & Authorization
Currently, the API does not implement authentication or authorization. The `changedBy` field in requests is used for audit logging purposes.

## Rate Limiting
No rate limiting is currently implemented.

## API Versioning
Current API version is v1. All endpoints are unversioned and considered stable.

## Order Status Flow
```
PENDING_PAYMENT → PAID → PROCESSING_IN_WAREHOUSE → SHIPPED → DELIVERED
                ↓
            CANCELLED (only from PENDING_PAYMENT or PAID)
```

## Return Status Flow
```
REQUESTED → APPROVED → IN_TRANSIT → RECEIVED → COMPLETED
         ↓
       REJECTED
```
