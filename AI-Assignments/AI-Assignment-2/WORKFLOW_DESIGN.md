# Workflow Design & Implementation

This document provides a comprehensive explanation of the Order & Returns Management System implementation, including detailed state machine diagrams, database schema relationships, business logic, and workflow orchestration.

## Table of Contents
1. [System Overview](#system-overview)
2. [Order State Machine](#order-state-machine)
3. [Return State Machine](#return-state-machine)
4. [Database Schema Design](#database-schema-design)
5. [Business Logic Implementation](#business-logic-implementation)
6. [Background Job Workflows](#background-job-workflows)
7. [Integration Points](#integration-points)
8. [Error Handling and Recovery](#error-handling-and-recovery)

## System Overview

The Order & Returns Management System is built around well-defined state machines that govern the lifecycle of orders and returns. The system ensures data consistency, provides comprehensive audit trails, and handles complex business workflows through asynchronous job processing.

### Key Design Principles
- **State-Driven Architecture**: All business logic is centered around state transitions
- **Audit-First Design**: Every state change is logged for compliance and debugging
- **Async Processing**: Time-consuming operations are handled asynchronously
- **Idempotent Operations**: All operations can be safely retried
- **Event-Driven Integration**: State changes trigger downstream processes

## Order State Machine

### Order State Diagram

```
┌─────────────────┐
│ PENDING_PAYMENT │ ◄─── Initial State (Order Created)
└─────────┬───────┘
          │ Payment Confirmed
          ▼
┌─────────────────┐
│      PAID       │
└─────────┬───────┘
          │ Warehouse Processing
          ▼
┌─────────────────┐
│PROCESSING_IN_   │
│   WAREHOUSE     │
└─────────┬───────┘
          │ Item Shipped
          ▼
┌─────────────────┐      ┌─────────────────┐
│    SHIPPED      │────► │ PDF Invoice     │ (Background Job)
└─────────┬───────┘      │ Generated &     │
          │              │ Emailed         │
          │              └─────────────────┘
          │ Delivery Confirmed
          ▼
┌─────────────────┐
│   DELIVERED     │ ◄─── Eligible for Returns
└─────────────────┘

┌─────────────────┐
│   CANCELLED     │ ◄─── From PENDING_PAYMENT or PAID only
└─────────────────┘
```

### Order State Transitions

| From State | To State | Trigger | Business Rules |
|------------|----------|---------|----------------|
| PENDING_PAYMENT | PAID | Payment confirmation | Must have valid payment |
| PENDING_PAYMENT | CANCELLED | Customer/Admin action | Allowed anytime before processing |
| PAID | PROCESSING_IN_WAREHOUSE | Warehouse pickup | Payment must be confirmed |
| PAID | CANCELLED | Customer/Admin action | Allowed before warehouse processing |
| PROCESSING_IN_WAREHOUSE | SHIPPED | Item dispatched | Triggers invoice generation job |
| SHIPPED | DELIVERED | Delivery confirmation | Updates delivery timestamp |

### Order State Validation Rules

**Business Constraints:**
- Orders can only be cancelled from `PENDING_PAYMENT` or `PAID` states
- Returns can only be initiated for `DELIVERED` orders
- State transitions must follow the defined sequence
- Each transition requires a `changedBy` actor for audit purposes

**Implementation in Code:**
```java
public enum OrderStatus {
    PENDING_PAYMENT,    // Initial state
    PAID,              // Payment received
    PROCESSING_IN_WAREHOUSE, // Being prepared for shipping
    SHIPPED,           // Dispatched to customer
    DELIVERED,         // Successfully delivered
    CANCELLED          // Cancelled by customer or system
}

// Validation logic in OrderService
public boolean isValidTransition(OrderStatus from, OrderStatus to) {
    return switch (from) {
        case PENDING_PAYMENT -> to == PAID || to == CANCELLED;
        case PAID -> to == PROCESSING_IN_WAREHOUSE || to == CANCELLED;
        case PROCESSING_IN_WAREHOUSE -> to == SHIPPED;
        case SHIPPED -> to == DELIVERED;
        default -> false;
    };
}
```

## Return State Machine

### Return State Diagram

```
Order (DELIVERED) ────► Return Request
                             │
                             ▼
                    ┌─────────────────┐
                    │   REQUESTED     │ ◄─── Initial State
                    └─────────┬───────┘
                             │ Admin Review
                    ┌────────┴────────┐
                    ▼                 ▼
          ┌─────────────────┐  ┌─────────────────┐
          │    APPROVED     │  │    REJECTED     │ ◄─── Terminal State
          └─────────┬───────┘  └─────────────────┘
                   │ Customer Ships Back
                   ▼
          ┌─────────────────┐
          │   IN_TRANSIT    │
          └─────────┬───────┘
                   │ Warehouse Receives
                   ▼
          ┌─────────────────┐
          │    RECEIVED     │
          └─────────┬───────┘
                   │ Processing Complete
                   ▼
          ┌─────────────────┐      ┌─────────────────┐
          │   COMPLETED     │────► │ Refund          │ (Background Job)
          └─────────────────┘      │ Processed       │
                                   └─────────────────┘
```

### Return State Transitions

| From State | To State | Trigger | Business Rules |
|------------|----------|---------|----------------|
| REQUESTED | APPROVED | Admin approval | Must validate return eligibility |
| REQUESTED | REJECTED | Admin rejection | Can reject with reason |
| APPROVED | IN_TRANSIT | Customer ships back | Tracking information captured |
| IN_TRANSIT | RECEIVED | Warehouse receives item | Item inspection completed |
| RECEIVED | COMPLETED | Processing finished | Triggers refund job |

### Return Eligibility Rules

**Business Constraints:**
- Returns can only be initiated for orders in `DELIVERED` state
- Each order can have multiple return requests (partial returns)
- Return approval requires manual intervention
- Rejected returns are terminal states
- Completed returns trigger automatic refund processing

**Implementation in Code:**
```java
public enum ReturnStatus {
    REQUESTED,    // Initial return request
    APPROVED,     // Approved by admin
    REJECTED,     // Rejected by admin (terminal)
    IN_TRANSIT,   // Customer shipped back
    RECEIVED,     // Received at warehouse
    COMPLETED     // Processing complete (triggers refund)
}

// Transition validation
public boolean canTransitionTo(ReturnStatus target) {
    return switch (this) {
        case REQUESTED -> target == APPROVED || target == REJECTED;
        case APPROVED -> target == IN_TRANSIT;
        case IN_TRANSIT -> target == RECEIVED;
        case RECEIVED -> target == COMPLETED;
        default -> false; // REJECTED and COMPLETED are terminal
    };
}
```

## Database Schema Design

### Entity Relationship Diagram

```
┌─────────────────┐       ┌─────────────────┐
│     orders      │       │    returns      │
├─────────────────┤       ├─────────────────┤
│ id (UUID) PK    │       │ id (UUID) PK    │
│ transaction_id  │       │ order_id (FK)   │──┐
│ customer_id     │       │ status          │  │
│ status          │       │ created_at      │  │
│ shipping_addr   │       └─────────────────┘  │
│ email           │                            │
│ failure_reason  │       ┌─────────────────┐  │
│ product_id      │       │ state_history   │  │
│ quantity        │       ├─────────────────┤  │
│ price           │       │ id (UUID) PK    │  │
│ created_at      │   ┌───│ transaction_id  │  │
│ updated_at      │   │   │ entity_type     │  │
└─────────────────┘   │   │ entity_id (FK)  │◄─┤
          │           │   │ state           │  │
          └───────────┼───│ changed_by      │  │
                      │   │ timestamp       │  │
┌─────────────────┐   │   │ created_at      │  │
│return_state_    │   │   │ updated_at      │  │
│    change       │   │   │ notes           │  │
├─────────────────┤   │   └─────────────────┘  │
│ id (UUID) PK    │   │                        │
│ return_id (FK)  │───┘   ┌─────────────────┐  │
│ from_status     │       │ job_execution   │  │
│ to_status       │       ├─────────────────┤  │
│ updated_at      │       │ id (UUID) PK    │  │
│ changed_by      │       │ job_type        │  │
└─────────────────┘       │ entity_id (FK)  │◄─┘
                          │ status          │
                          │ retry_count     │
                          │ max_retries     │
                          │ last_attempt    │
                          │ next_attempt    │
                          │ error_message   │
                          │ result          │
                          │ created_at      │
                          │ updated_at      │
                          │ idempotency_key │
                          └─────────────────┘
```

### Table Specifications

#### orders Table
```sql
CREATE TABLE orders (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    shipping_address TEXT NOT NULL,
    email VARCHAR(255),
    failure_reason TEXT,
    product_id UUID NOT NULL,
    quantity INT NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
```
**Purpose**: Stores complete order information and current state
**Key Features**: UUID primary keys, audit timestamps, flexible address storage

#### returns Table
```sql
CREATE TABLE returns (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
```
**Purpose**: Tracks return requests linked to specific orders
**Key Features**: Foreign key relationship to orders, simple status tracking

#### state_history Table
```sql
CREATE TABLE state_history (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    state VARCHAR(50) NOT NULL,
    changed_by UUID,
    timestamp TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    notes TEXT
);
```
**Purpose**: Universal audit trail for both orders and returns
**Key Features**: Polymorphic design, comprehensive audit information

#### return_state_history Table
```sql
CREATE TABLE return_state_history (
    id UUID PRIMARY KEY,
    return_id UUID NOT NULL,
    from_status VARCHAR(50) NOT NULL,
    to_status VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    changed_by UUID NOT NULL
);
```
**Purpose**: Specific return state transition tracking
**Key Features**: Before/after state capture, change attribution

#### job_execution Table
```sql
CREATE TABLE job_execution (
    id UUID PRIMARY KEY,
    job_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    retry_count INT DEFAULT 0,
    max_retries INT DEFAULT 3,
    last_attempt TIMESTAMP,
    next_attempt TIMESTAMP,
    error_message TEXT,
    result TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    idempotency_key VARCHAR(255) UNIQUE
);
```
**Purpose**: Background job execution tracking and retry management
**Key Features**: Retry logic, idempotency, comprehensive error tracking

### Database Relationships

1. **orders → state_history**: One-to-Many (audit trail)
2. **returns → state_history**: One-to-Many (audit trail)
3. **orders → returns**: One-to-Many (order can have multiple returns)
4. **returns → return_state_history**: One-to-Many (detailed return tracking)
5. **orders/returns → job_execution**: One-to-Many (background jobs)

### Indexing Strategy

```sql
-- Performance optimization indexes
CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_returns_order_id ON returns(order_id);
CREATE INDEX idx_returns_status ON returns(status);
CREATE INDEX idx_state_history_entity_id ON state_history(entity_id);
CREATE INDEX idx_state_history_entity_type ON state_history(entity_type);
CREATE INDEX idx_return_state_history_return_id ON return_state_history(return_id);
CREATE INDEX idx_job_execution_status ON job_execution(status);
CREATE INDEX idx_job_execution_entity_id ON job_execution(entity_id);
```

## Business Logic Implementation

### Order Service Implementation

```java
@Service
@Transactional
public class OrderService {
    
    public Order transitionOrderState(UUID orderId, OrderStatus newState, UUID changedBy) {
        Order order = findOrderById(orderId);
        OrderStatus currentState = order.getStatus();
        
        // Validate transition
        if (!isValidTransition(currentState, newState)) {
            throw new IllegalStateException(
                "Invalid transition from " + currentState + " to " + newState);
        }
        
        // Update order state
        order.setStatus(newState);
        order.setUpdatedAt(Instant.now());
        Order savedOrder = orderRepository.save(order);
        
        // Log state change for audit
        logOrderStateChange(order, changedBy);
        
        // Trigger background jobs if needed
        triggerBackgroundJobs(order, newState);
        
        return savedOrder;
    }
    
    private void triggerBackgroundJobs(Order order, OrderStatus newState) {
        if (newState == OrderStatus.SHIPPED) {
            invoiceJobService.scheduleInvoiceGeneration(order.getId());
        }
    }
}
```

### Return Service Implementation

```java
@Service
@Transactional
public class ReturnService {
    
    public Return initiateReturn(UUID orderId, UUID changedBy) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        
        // Validate return eligibility
        if (!isEligibleForReturn(order.getStatus())) {
            throw new IllegalStateException("Return not allowed. Order must be DELIVERED.");
        }
        
        // Create return request
        Return returnObj = new Return();
        returnObj.setId(UUID.randomUUID());
        returnObj.setOrderId(orderId);
        returnObj.setStatus(ReturnStatus.REQUESTED);
        returnObj.setCreatedAt(Instant.now());
        
        Return savedReturn = returnRepository.save(returnObj);
        
        // Log return initiation
        logReturnStateChange(savedReturn, changedBy);
        
        return savedReturn;
    }
    
    public Return transitionReturnState(UUID returnId, ReturnStatus newState, UUID changedBy) {
        Return returnObj = findReturnById(returnId);
        
        // Validate transition
        if (!returnObj.getStatus().canTransitionTo(newState)) {
            throw new IllegalStateException("Invalid return state transition");
        }
        
        returnObj.setStatus(newState);
        Return savedReturn = returnRepository.save(returnObj);
        
        // Log state change
        logReturnStateChange(savedReturn, changedBy);
        
        // Trigger refund processing if completed
        if (newState == ReturnStatus.COMPLETED) {
            Order order = orderRepository.findById(returnObj.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
            refundJobService.processRefundAsync(returnObj, order, changedBy);
        }
        
        return savedReturn;
    }
}
```

## Background Job Workflows

### Invoice Generation Workflow

```
Order Status → SHIPPED
       │
       ▼
┌─────────────────┐
│ Create Job      │
│ Type: INVOICE_  │
│ GENERATION      │
└─────────┬───────┘
          │
          ▼
┌─────────────────┐    ┌─────────────────┐
│ Generate PDF    │───►│ PDF Created     │
│ Invoice         │    │ Successfully    │
└─────────┬───────┘    └─────────────────┘
          │                      │
          ▼                      │
┌─────────────────┐              │
│ Send Email      │◄─────────────┘
│ with Invoice    │
└─────────┬───────┘
          │
          ▼
┌─────────────────┐
│ Job Completed   │
│ Status: SUCCESS │
└─────────────────┘
```

### Refund Processing Workflow

```
Return Status → COMPLETED
       │
       ▼
┌─────────────────┐
│ Create Job      │
│ Type: REFUND_   │
│ PROCESSING      │
└─────────┬───────┘
          │
          ▼
┌─────────────────┐    ┌─────────────────┐
│ Call Mock       │───►│ Payment Gateway │
│ Payment API     │    │ Response        │
└─────────┬───────┘    └─────────────────┘
          │                      │
          ▼                      │
┌─────────────────┐              │
│ Update Return   │◄─────────────┘
│ with Refund ID  │
└─────────┬───────┘
          │
          ▼
┌─────────────────┐
│ Send Email      │
│ Confirmation    │
└─────────┬───────┘
          │
          ▼
┌─────────────────┐
│ Job Completed   │
│ Status: SUCCESS │
└─────────────────┘
```

### Job Retry Logic

```java
@Service
public class JobSchedulerService {
    
    @Scheduled(fixedRateString = "${scheduler.process-jobs-rate}")
    public void processJobs() {
        List<JobExecution> pendingJobs = jobExecutionRepository
            .findByStatusOrderByCreatedAtAsc(JobStatus.PENDING);
            
        for (JobExecution job : pendingJobs) {
            try {
                processJob(job);
                job.setStatus(JobStatus.COMPLETED);
                job.setResult("Job completed successfully");
            } catch (Exception e) {
                handleJobFailure(job, e);
            }
            
            jobExecutionRepository.save(job);
        }
    }
    
    private void handleJobFailure(JobExecution job, Exception e) {
        job.setRetryCount(job.getRetryCount() + 1);
        job.setErrorMessage(e.getMessage());
        job.setLastAttempt(Instant.now());
        
        if (job.getRetryCount() >= job.getMaxRetries()) {
            job.setStatus(JobStatus.FAILED);
        } else {
            job.setStatus(JobStatus.PENDING);
            // Exponential backoff
            long delayMinutes = (long) Math.pow(2, job.getRetryCount()) * 5;
            job.setNextAttempt(Instant.now().plusSeconds(delayMinutes * 60));
        }
    }
}
```

## Integration Points

### Email Service Integration

```java
@Service
public class EmailService {
    
    public void sendOrderConfirmation(Order order) {
        EmailTemplate template = emailTemplateService
            .getTemplate("ORDER_CONFIRMATION");
        
        String content = template.render(Map.of(
            "orderNumber", order.getId(),
            "customerName", getCustomerName(order.getCustomerId()),
            "orderTotal", order.getPrice() * order.getQuantity()
        ));
        
        sendEmail(order.getEmail(), "Order Confirmation", content);
    }
    
    public void sendInvoiceEmail(Order order, byte[] pdfInvoice) {
        String subject = "Invoice for Order " + order.getId();
        String content = generateInvoiceEmailContent(order);
        
        EmailMessage message = EmailMessage.builder()
            .to(order.getEmail())
            .subject(subject)
            .content(content)
            .attachment("invoice.pdf", pdfInvoice)
            .build();
            
        sendEmail(message);
    }
}
```

### Mock Payment Gateway Integration

```java
@RestController
@RequestMapping("/mock-payment")
public class MockPaymentController {
    
    @PostMapping("/refund")
    public ResponseEntity<RefundResponse> processRefund(@RequestBody RefundRequest request) {
        // Simulate processing time
        simulateProcessingDelay();
        
        // Simulate success/failure based on business rules
        boolean success = shouldRefundSucceed(request);
        
        RefundResponse response = RefundResponse.builder()
            .success(success)
            .status(success ? "COMPLETED" : "FAILED")
            .message(generateRefundMessage(request, success))
            .refundId(success ? generateRefundId() : null)
            .amount(request.getAmount())
            .transactionId(request.getTransactionId())
            .timestamp(Instant.now().toString())
            .build();
            
        return ResponseEntity.ok(response);
    }
}
```

## Error Handling and Recovery

### Validation and Error Responses

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStateTransition(
            IllegalStateException e, HttpServletRequest request) {
        
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(422)
            .error("Unprocessable Entity")
            .message(e.getMessage())
            .path(request.getRequestURI())
            .build();
            
        return ResponseEntity.status(422).body(error);
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            IllegalArgumentException e, HttpServletRequest request) {
        
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(404)
            .error("Not Found")
            .message(e.getMessage())
            .path(request.getRequestURI())
            .build();
            
        return ResponseEntity.status(404).body(error);
    }
}
```

### Transaction Management

```java
@Service
@Transactional
public class OrderService {
    
    @Transactional(rollbackFor = Exception.class)
    public Order transitionOrderState(UUID orderId, OrderStatus newState, UUID changedBy) {
        // All database operations in this method are part of a single transaction
        // If any operation fails, the entire transaction is rolled back
        
        Order order = findOrderById(orderId);
        // ... state transition logic
        Order savedOrder = orderRepository.save(order);
        logOrderStateChange(order, changedBy);
        
        return savedOrder;
    }
}
```

### Recovery Mechanisms

1. **Database Transactions**: Ensure data consistency across multiple table operations
2. **Job Retry Logic**: Automatic retry with exponential backoff for failed background jobs
3. **Idempotency**: All operations can be safely retried without side effects
4. **Manual Intervention**: Failed jobs can be manually retried through API endpoints
5. **Comprehensive Logging**: Detailed logs for debugging and troubleshooting

This workflow design ensures reliable, scalable, and maintainable order and return processing with comprehensive audit trails and robust error handling.
