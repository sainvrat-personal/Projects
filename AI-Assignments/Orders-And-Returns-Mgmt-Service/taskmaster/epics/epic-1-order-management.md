# Epic 1: Order Management

## Story 1.1: Implement Order State Machine
- **Task:** Design order state model and transitions
  - **Output:** State diagram and model for order lifecycle (PENDING_PAYMENT → PAID → PROCESSING_IN_WAREHOUSE → SHIPPED → DELIVERED)
- **Task:** Implement state transition logic and validation
  - **Output:** Working code enforcing valid transitions and preventing invalid ones
- **Task:** Log all state changes for audit trail
  - **Output:** Persistent audit log entries for every state change

## Story 1.2: Order Cancellation
- **Task:** Implement cancellation logic (from PENDING_PAYMENT or PAID)
  - **Output:** Code to cancel orders only in allowed states
- **Task:** Update audit log on cancellation
  - **Output:** Audit log entry for each cancellation
