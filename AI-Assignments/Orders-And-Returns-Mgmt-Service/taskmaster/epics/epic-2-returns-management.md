# Epic 2: Returns Management

## Story 2.1: Implement Return State Machine
- **Task:** Design return state model and transitions
  - **Output:** State diagram and model for returns (REQUESTED → APPROVED/REJECTED → IN_TRANSIT → RECEIVED → COMPLETED)
- **Task:** Implement state transition logic and validation
  - **Output:** Working code enforcing valid transitions and preventing invalid ones
- **Task:** Log all state changes for audit trail
  - **Output:** Persistent audit log entries for every state change

  stateDiagram-v2
    [*] --> REQUESTED
    REQUESTED --> APPROVED
    REQUESTED --> REJECTED
    APPROVED --> IN_TRANSIT
    IN_TRANSIT --> RECEIVED
    RECEIVED --> COMPLETED

## Story 2.2: Return Eligibility
- **Task:** Enforce returns only for DELIVERED orders
  - **Output:** Code that checks order state before allowing returns
