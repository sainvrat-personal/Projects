package com.example.orderservice.controller;

import com.example.orderservice.dto.OrderCancellationRequest;
import com.example.orderservice.dto.OrderStateTransitionRequest;
import com.example.orderservice.dto.OrderStatus;
import com.example.orderservice.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class OrderControllerUnitTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    private UUID orderId;
    private UUID changedBy;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        orderId = UUID.randomUUID();
        changedBy = UUID.randomUUID();
    }

    @Test
    void placeOrder_nullRequest_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> orderController.placeOrder(null)
        );
        assertEquals("Request body is required.", ex.getMessage());
        verify(orderService, never()).createOrder(any());
    }

    @Test
    void updateOrderState_nullRequest_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> orderController.updateOrderState(orderId, null)
        );
        assertEquals("Request body is required.", ex.getMessage());
        verify(orderService, never()).transitionOrderState(any(), any(), any());
    }

    @Test
    void updateOrderState_nullNewState_throwsIllegalArgumentException() {
        OrderStateTransitionRequest request = new OrderStateTransitionRequest();
        request.setNewState(null);
        request.setChangedBy(changedBy);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> orderController.updateOrderState(orderId, request)
        );
        assertEquals("New state is required.", ex.getMessage());
        verify(orderService, never()).transitionOrderState(any(), any(), any());
    }

    @Test
    void updateOrderState_nullChangedBy_throwsIllegalArgumentException() {
        OrderStateTransitionRequest request = new OrderStateTransitionRequest();
        request.setNewState(OrderStatus.PAID);
        request.setChangedBy(null);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> orderController.updateOrderState(orderId, request)
        );
        assertEquals("changedBy is required for auditing.", ex.getMessage());
        verify(orderService, never()).transitionOrderState(any(), any(), any());
    }

    @Test
    void cancelOrder_nullRequest_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> orderController.cancelOrder(orderId, null)
        );
        assertEquals("Request body is required.", ex.getMessage());
        verify(orderService, never()).cancelOrder(any(), any(), any());
    }

    @Test
    void cancelOrder_nullChangedBy_throwsIllegalArgumentException() {
        OrderCancellationRequest request = new OrderCancellationRequest();
        request.setChangedBy(null);
        request.setReason("Customer request");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> orderController.cancelOrder(orderId, request)
        );
        assertEquals("changedBy is required for auditing.", ex.getMessage());
        verify(orderService, never()).cancelOrder(any(), any(), any());
    }

    @Test
    void cancelOrder_blankReason_throwsIllegalArgumentException() {
        OrderCancellationRequest request = new OrderCancellationRequest();
        request.setChangedBy(changedBy);
        request.setReason("   ");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> orderController.cancelOrder(orderId, request)
        );
        assertEquals("Cancellation reason is required.", ex.getMessage());
        verify(orderService, never()).cancelOrder(any(), any(), any());
    }
}

