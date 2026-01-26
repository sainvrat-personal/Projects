# Testing Strategy & Coverage

This document outlines the testing approach for the Order & Returns Management System, including both positive and negative test scenarios.

## Overview

The application is thoroughly tested through a combination of unit tests, integration tests, and API tests to ensure all components work correctly and the business rules are enforced properly.

## Test Coverage

Our test suite aims to achieve high coverage across all critical components:

- **Controller Layer**: Testing API endpoints, validation, and error responses
- **Service Layer**: Testing business logic, state transitions, and error handling
- **Repository Layer**: Testing data access and persistence
- **Background Jobs**: Testing job creation, processing, and retries

Current test coverage metrics (measured via JaCoCo):
- **Overall Coverage**: ~90%+
- **Controller Layer**: ~90%+
- **Service Layer**: ~90%+
- **Repository Layer**: ~85%+
- **Background Jobs**: ~85%+

## Positive Test Cases

### Order Management

1. **Order Creation**
   - Creating a new order with valid data
   - Verifying order is in PENDING_PAYMENT state

2. **Order State Transitions**
   - Complete order lifecycle: PENDING_PAYMENT → PAID → PROCESSING_IN_WAREHOUSE → SHIPPED → DELIVERED
   - Order cancellation from PENDING_PAYMENT state
   - Order cancellation from PAID state

3. **Order Queries**
   - Fetching order details
   - Listing orders with filtering
   - Retrieving order state history

### Return Management

1. **Return Creation**
   - Initiating a return for a delivered order within the return window
   - Verifying return is in REQUESTED state

2. **Return State Transitions**
   - Approval flow: REQUESTED → APPROVED → IN_TRANSIT → RECEIVED → COMPLETED
   - Rejection flow: REQUESTED → REJECTED

3. **Return Queries**
   - Fetching return details
   - Listing returns with filtering
   - Retrieving return state history

### Background Jobs

1. **Invoice Generation**
   - Verifying job creation when order transitions to SHIPPED
   - Successful job execution and invoice generation
   - Verifying email notification

2. **Refund Processing**
   - Verifying job creation when return completes
   - Successful refund processing
   - Verifying confirmation notifications

## Negative Test Cases

### Invalid Inputs

1. **Validation Failures**
   - Missing required fields
   - Invalid data formats
   - Out-of-range values

2. **Duplicate Prevention**
   - Submitting the same order twice
   - Initiating multiple returns for the same order item

### Invalid State Transitions

1. **Order State Violations**
   - Attempting to transition from PENDING_PAYMENT directly to SHIPPED
   - Attempting to cancel an already DELIVERED order
   - Attempting to deliver an order that hasn't been shipped

2. **Return State Violations**
   - Attempting to transition from REQUESTED directly to RECEIVED
   - Attempting to approve a return for an order not in DELIVERED state
   - Attempting to complete a return that hasn't been received

### Business Rule Violations

1. **Order Rules**
   - Creating an order with no items
   - Creating an order with invalid payment information
   - Shipping an order with insufficient inventory

2. **Return Rules**
   - Initiating a return after the return window has expired
   - Initiating a return for an order that doesn't exist
   - Initiating multiple returns for the same order (when not allowed)

### Error Handling

1. **API Error Responses**
   - Verifying correct HTTP status codes for different errors
   - Validating error response structure and messages
   - Testing error details for validation failures

2. **Concurrent Operations**
   - Multiple simultaneous state transition attempts
   - Race conditions in job processing

### Background Job Failures

1. **Job Retry Mechanism**
   - Verifying jobs are retried when they fail
   - Testing exponential backoff behavior
   - Confirming max retry limits

2. **Failed Job Handling**
   - Testing manual retry of failed jobs
   - Verifying error details are properly recorded
   - Testing idempotency for retried jobs

## Example Test Case: Invalid State Transition

```java
@Test
public void testInvalidOrderStateTransition() {
    // Create a test order in PENDING_PAYMENT state
    Order order = createTestOrder();
    
    // Attempt to transition directly to SHIPPED (invalid)
    OrderStateTransitionRequest request = new OrderStateTransitionRequest();
    request.setNewState(OrderState.SHIPPED);
    
    // Execute request and verify error response
    mockMvc.perform(post("/orders/{orderId}/transitions", order.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.error").value("Conflict"))
            .andExpect(jsonPath("$.message").value("Invalid state transition"))
            .andExpect(jsonPath("$.details[0]").value(
                    "Cannot transition order from PENDING_PAYMENT to SHIPPED"));
}
```

## Example Test Case: Duplicate Return Prevention

```java
@Test
public void testDuplicateReturnPrevention() {
    // Create a test order in DELIVERED state
    Order order = createAndDeliverTestOrder();
    
    // Create first return request (should succeed)
    ReturnRequest firstRequest = new ReturnRequest();
    firstRequest.setOrderId(order.getId());
    firstRequest.setReason("Damaged product");
    
    mockMvc.perform(post("/returns")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(firstRequest)))
            .andExpect(status().isCreated());
    
    // Create second return request for same order (should fail)
    ReturnRequest secondRequest = new ReturnRequest();
    secondRequest.setOrderId(order.getId());
    secondRequest.setReason("Changed my mind");
    
    mockMvc.perform(post("/returns")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(secondRequest)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.error").value("Conflict"))
            .andExpect(jsonPath("$.message").value("Business rule violation"))
            .andExpect(jsonPath("$.details[0]").value(
                    "Return already exists for this order"));
}
```

## Running Tests

To run the entire test suite:

```bash
mvn test
```

To run a specific test class:

```bash
mvn test -Dtest=OrderControllerIntegrationTest
```

To generate a coverage report:

```bash
mvn jacoco:report
```

The coverage report will be available at: `target/site/jacoco/index.html`

## Test Data Setup

Our tests use a combination of:

1. **Test Fixtures**: Predefined data for common test scenarios
2. **Factory Methods**: Helper methods to create test entities with specific states
3. **Test Database**: H2 in-memory database for integration tests
4. **Mocks**: For external dependencies like email and payment services

## Continuous Integration

All tests are run as part of our CI pipeline to ensure code changes don't break existing functionality.