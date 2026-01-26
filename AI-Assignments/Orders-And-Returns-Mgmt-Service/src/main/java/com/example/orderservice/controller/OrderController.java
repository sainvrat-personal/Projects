
package com.example.orderservice.controller;

import com.example.orderservice.dto.CreateOrderRequest;
import com.example.orderservice.dto.CreateOrderResponse;
import com.example.orderservice.dto.OrderCancellationRequest;
import com.example.orderservice.dto.OrderStateTransitionRequest;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.StateHistory;
import com.example.orderservice.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Place a new order (initial state: PENDING_PAYMENT)
     * 
     * @param request CreateOrderRequest containing customerId, items, and
     *                shippingAddress
     * @return CreateOrderResponse with orderId, status, and createdAt
     */
    @PostMapping("/order")
    public ResponseEntity<?> placeOrder(@Valid @RequestBody CreateOrderRequest request) {
        // Basic null check – let global handler format the error
        if (request == null) {
            throw new IllegalArgumentException("Request body is required.");
        }

        // Validate email format for Gmail (soft warning only)
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            String email = request.getEmail().trim().toLowerCase();
            if (!email.matches("^[a-zA-Z0-9._%+-]+@gmail\\.com$")) {
                System.out.println("Warning: Non-Gmail address used: " + email);
            }
        }

        Order order = orderService.createOrder(request);
        CreateOrderResponse response = new CreateOrderResponse(
                order.getId(),
                order.getStatus(),
                order.getCreatedAt());
        return ResponseEntity.ok(response);
    }

    /**
     * Update the state of an order
     * 
     * @param orderId The ID of the order to update
     * @param request OrderStateTransitionRequest containing the new state and who
     *                made the change
     * @return Updated Order or error response
     */
    @PatchMapping("/{orderId}/state")
    public ResponseEntity<?> updateOrderState(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderStateTransitionRequest request) {
        // Validate input before delegating to service
        if (request == null) {
            throw new IllegalArgumentException("Request body is required.");
        }
        if (request.getNewState() == null) {
            throw new IllegalArgumentException("New state is required.");
        }
        if (request.getChangedBy() == null) {
            throw new IllegalArgumentException("changedBy is required for auditing.");
        }

        Order updatedOrder = orderService.transitionOrderState(orderId, request.getNewState(),
                request.getChangedBy());
        return ResponseEntity.ok(updatedOrder);
    }

    /**
     * Cancel an order (only allowed for PENDING_PAYMENT or PAID orders)
     * 
     * @param orderId The ID of the order to cancel
     * @param request OrderCancellationRequest containing who made the change and
     *                reason
     * @return Cancelled Order or error response
     */
    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderCancellationRequest request) {
        // Validate input before delegating to service
        if (request == null) {
            throw new IllegalArgumentException("Request body is required.");
        }
        if (request.getChangedBy() == null) {
            throw new IllegalArgumentException("changedBy is required for auditing.");
        }
        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new IllegalArgumentException("Cancellation reason is required.");
        }

        Order cancelledOrder = orderService.cancelOrder(orderId, request.getReason(), request.getChangedBy());
        return ResponseEntity.ok(cancelledOrder);
    }

    /**
     * Get the state history for an order
     * 
     * @param orderId The ID of the order
     * @return List of state history entries or error response
     */
    @GetMapping("/{orderId}/state-history")
    public ResponseEntity<?> getOrderStateHistory(@PathVariable UUID orderId) {
        List<StateHistory> history = orderService.getOrderStateHistory(orderId);
        return ResponseEntity.ok(history);
    }
}
