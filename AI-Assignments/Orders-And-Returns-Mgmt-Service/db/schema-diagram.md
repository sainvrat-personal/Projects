# Database Schema Diagram

```mermaid
erDiagram
    ORDERS ||--o{ RETURNS : has
    RETURNS ||--o{ RETURN_STATE_HISTORY : tracks_state_changes
    ORDERS ||--o{ STATE_HISTORY : logs_state
    RETURNS ||--o{ STATE_HISTORY : logs_state
    ORDERS ||--o{ JOB_EXECUTION : has_jobs
    RETURNS ||--o{ JOB_EXECUTION : has_jobs

    ORDERS {
        UUID id PK
        UUID transaction_id
        UUID customer_id
        String status
        String shipping_address
        String email
        String failure_reason
        UUID product_id
        int quantity
        decimal price
        Timestamp created_at
        Timestamp updated_at
    }

    RETURNS {
        UUID id PK
        UUID order_id FK
        String status
        Timestamp created_at
    }

    STATE_HISTORY {
        UUID id PK
        UUID transaction_id
        String entity_type
        UUID entity_id
        String state
        UUID changed_by
        Timestamp timestamp
        Timestamp created_at
        Timestamp updated_at
        String notes
    }

    RETURN_STATE_HISTORY {
        BigSerial id PK
        UUID return_id FK
        String from_status
        String to_status
        Timestamp updated_at
        UUID customer_id
    }

    JOB_EXECUTION {
        UUID id PK
        String job_type
        UUID entity_id
        String status
        int retry_count
        int max_retries
        Timestamp last_attempt
        Timestamp next_attempt
        String error_message
        String result
        Timestamp created_at
        Timestamp updated_at
        String idempotency_key
    }
```

## State Machines

### Order Status Flow
```
PENDING -> PAID -> PROCESSING_IN_WAREHOUSE -> SHIPPED -> DELIVERED
   |          |
   v          v
CANCELLED  CANCELLED
```

### Return Status Flow
```
REQUESTED -> APPROVED -> IN_TRANSIT -> RECEIVED -> COMPLETED
      |
      v
   REJECTED
```

## Key Entities

- **Orders**: Core entity representing customer orders with product, pricing, and shipping details
- **Returns**: Represents return requests for orders with their current processing status
- **State_History**: Maintains a complete audit log of all state changes for orders and returns
- **Return_State_History**: Tracks detailed state transitions specifically for returns
- **Job_Execution**: Manages background jobs like invoice generation and refund processing with retry capability

## Indexes
The schema includes indexes on frequently queried columns to improve performance:
- Customer IDs
- Order and return status
- Entity IDs in state history
- Job status and associated entity IDs
