# State Transition System

This document explains the state transition system used in the Order & Returns Management application.

## Overview

The application implements a rigorous state machine approach for both orders and returns. This ensures that:

1. Entities can only move through predefined, valid state transitions
2. Each state transition is audited
3. Business rules are enforced during transitions
4. Actions are triggered automatically by specific transitions

## Order State Machine

### Order States

| State | Description |
|-------|-------------|
| `PENDING_PAYMENT` | Initial state when order is first created and awaiting payment |
| `PAID` | Payment has been received but order not yet processed |
| `PROCESSING_IN_WAREHOUSE` | Order is being prepared for shipping |
| `SHIPPED` | Order has been shipped to the customer |
| `DELIVERED` | Order has been delivered to the customer |
| `CANCELLED` | Order has been cancelled |

### Valid Order Transitions

```
PENDING_PAYMENT → PAID → PROCESSING_IN_WAREHOUSE → SHIPPED → DELIVERED
       ↘             ↘                
        ↘             ↘                
         ↘             ↘                
          → → → → → → → → CANCELLED
```

### Transition Validations & Actions

| Transition | Validation Rules | Triggered Actions |
|------------|------------------|------------------|
| PENDING_PAYMENT → PAID | Payment confirmation required | Update inventory |
| PAID → PROCESSING_IN_WAREHOUSE | Inventory must be available | None |
| PROCESSING_IN_WAREHOUSE → SHIPPED | All items must be in stock | Generate invoice, Send email |
| SHIPPED → DELIVERED | None | None |
| PENDING_PAYMENT → CANCELLED | Cannot cancel if already shipped or delivered | None |
| PAID → CANCELLED | Cannot cancel if already in processing or shipped | Refund payment, Restore inventory |

## Return State Machine

### Return States

| State | Description |
|-------|-------------|
| `REQUESTED` | Initial state when return is first requested |
| `APPROVED` | Return has been approved but items not yet received |
| `REJECTED` | Return request has been rejected |
| `IN_TRANSIT` | Return items are in transit back to warehouse |
| `RECEIVED` | Return items have been received at warehouse |
| `COMPLETED` | Return process is complete and refund issued |

### Valid Return Transitions

```
                     ↗ REJECTED
                    ↗
REQUESTED → APPROVED → IN_TRANSIT → RECEIVED → COMPLETED
```

### Transition Validations & Actions

| Transition | Validation Rules | Triggered Actions |
|------------|------------------|------------------|
| REQUESTED → APPROVED | Order must exist and be in DELIVERED state, Return window not expired | Generate return label, Send email |
| REQUESTED → REJECTED | None | Send rejection email |
| APPROVED → IN_TRANSIT | Return label must be generated | None |
| IN_TRANSIT → RECEIVED | None | None |
| RECEIVED → COMPLETED | None | Process refund, Send confirmation email |

## Implementation Details

### State History Tracking

All state transitions are recorded in the `state_history` table:

```sql
CREATE TABLE state_history (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(50) NOT NULL,
    previous_state VARCHAR(50),
    new_state VARCHAR(50) NOT NULL,
    changed_by VARCHAR(100) NOT NULL,
    changed_at TIMESTAMP NOT NULL,
    reason TEXT
);
```

This provides a complete audit trail of all state changes, including:
- Who made the change
- When the change occurred
- The reason for the change (if provided)

### State Transition API

The application provides explicit REST endpoints for state transitions:

```
POST /orders/{orderId}/transitions
```

Example request body:
```json
{
  "newState": "SHIPPED",
  "reason": "All items in stock and packaged"
}
```

Response:
```json
{
  "orderId": "ORDER-12345",
  "previousState": "PROCESSING",
  "currentState": "SHIPPED",
  "transitionedAt": "2023-07-01T12:34:56.789Z"
}
```

### State Transition Service

The core state transition logic is implemented in service classes:

```java
@Service
@Transactional
public class OrderStateTransitionService {

    private final OrderRepository orderRepository;
    private final StateHistoryRepository historyRepository;
    private final InvoiceService invoiceService;
    private final EmailService emailService;
    
    // Constructor with dependencies...
    
    public Order transition(String orderId, OrderState newState, String reason) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        
        validateTransition(order, newState);
        
        OrderState previousState = order.getState();
        order.setState(newState);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository = orderRepository.save(order);
        
        // Record state transition in history
        StateHistory history = new StateHistory();
        history.setEntityType("ORDER");
        history.setEntityId(orderId);
        history.setPreviousState(previousState.name());
        history.setNewState(newState.name());
        history.setChangedBy("system"); // In a real app, this would be the authenticated user
        history.setChangedAt(LocalDateTime.now());
        history.setReason(reason);
        historyRepository.save(history);
        
        // Execute actions based on the new state
        executeStateActions(order, previousState, newState);
        
        return order;
    }
    
    private void validateTransition(Order order, OrderState newState) {
        OrderState currentState = order.getState();
        
        // Define valid transitions
        Map<OrderState, Set<OrderState>> validTransitions = new HashMap<>();
        validTransitions.put(OrderState.CREATED, Set.of(OrderState.PENDING_PAYMENT, OrderState.CANCELLED));
        validTransitions.put(OrderState.PENDING_PAYMENT, Set.of(OrderState.PAYMENT_RECEIVED, OrderState.CANCELLED));
        validTransitions.put(OrderState.PAYMENT_RECEIVED, Set.of(OrderState.PROCESSING, OrderState.CANCELLED));
        validTransitions.put(OrderState.PROCESSING, Set.of(OrderState.SHIPPED, OrderState.CANCELLED));
        validTransitions.put(OrderState.SHIPPED, Set.of(OrderState.DELIVERED));
        validTransitions.put(OrderState.DELIVERED, Collections.emptySet());
        validTransitions.put(OrderState.CANCELLED, Collections.emptySet());
        
        // Check if transition is valid
        if (!validTransitions.getOrDefault(currentState, Collections.emptySet()).contains(newState)) {
            throw new InvalidStateTransitionException(
                String.format("Cannot transition order from %s to %s", currentState, newState));
        }
        
        // Additional business rule validations
        if (newState == OrderState.SHIPPED) {
            validateShippingRequirements(order);
        }
        
        // More validations for other transitions...
    }
    
    private void executeStateActions(Order order, OrderState previousState, OrderState newState) {
        if (newState == OrderState.SHIPPED) {
            // Generate invoice
            invoiceService.generateInvoice(order);
            
            // Send shipping notification
            emailService.sendShippingNotification(order);
        }
        
        // More actions for other state transitions...
    }
    
    private void validateShippingRequirements(Order order) {
        // Check inventory availability, etc.
        // Throw BusinessRuleViolationException if requirements not met
    }
}
```

## Querying State History

The application provides endpoints to query the state history of an entity:

```
GET /orders/{orderId}/state-history
```

Response:
```json
[
  {
    "previousState": "CREATED",
    "newState": "PENDING_PAYMENT",
    "changedBy": "system",
    "changedAt": "2023-07-01T10:15:30.123Z",
    "reason": "Order placed by customer"
  },
  {
    "previousState": "PENDING_PAYMENT",
    "newState": "PAYMENT_RECEIVED",
    "changedBy": "payment-service",
    "changedAt": "2023-07-01T10:17:45.456Z",
    "reason": "Payment confirmed by payment gateway"
  },
  {
    "previousState": "PAYMENT_RECEIVED",
    "newState": "PROCESSING",
    "changedBy": "system",
    "changedAt": "2023-07-01T10:18:00.789Z",
    "reason": "Automatic transition after payment"
  }
]
```

## Best Practices

1. **Never Bypass the State Machine**:
   - Always use the transition service
   - Never update state directly in repositories

2. **Validate Before Transition**:
   - Check all business rules before changing state
   - Throw appropriate exceptions for invalid transitions

3. **Document State Machine**:
   - Keep state diagram updated
   - Document all valid transitions and their requirements

4. **Audit Everything**:
   - Record all state changes with metadata
   - Include reason for manual state changes

5. **Idempotent Operations**:
   - Handle duplicate transition requests gracefully
   - Return success for already-completed transitions