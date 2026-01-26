package com.example.orderservice.service;

import com.example.orderservice.dto.OrderStatus;
import com.example.orderservice.entity.Order;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.repository.StateHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderServiceTest {
    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private StateHistoryRepository stateHistoryRepository;
    
    @Mock
    private InvoiceJobService invoiceJobService;
    
    @Mock
    private EmailService emailService;

    @Mock
    private JobScheduler jobScheduler;
    
    @InjectMocks
    private OrderService orderService;

    private UUID orderId;
    private UUID changedBy;
    private Order order;
    private String cancellationReason = "Customer request";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        orderId = UUID.randomUUID();
        changedBy = UUID.randomUUID();
        order = new Order();
        order.setId(orderId);
        order.setTransactionId(UUID.randomUUID());
        order.setStatus(OrderStatus.PENDING_PAYMENT);
    }

    @Test
    void testCancelOrderFromPendingPayment() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        Order cancelled = orderService.cancelOrder(orderId, cancellationReason, changedBy);
        assertEquals(OrderStatus.CANCELLED, cancelled.getStatus());
    }

    @Test
    void testCancelOrderFromPaid() {
        order.setStatus(OrderStatus.PAID);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        Order cancelled = orderService.cancelOrder(orderId, cancellationReason, changedBy);
        assertEquals(OrderStatus.CANCELLED, cancelled.getStatus());
    }

    @Test
    void testCancelOrderFromInvalidStateThrows() {
        order.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        Exception ex = assertThrows(IllegalStateException.class, () ->
                orderService.cancelOrder(orderId, cancellationReason, changedBy));
        assertTrue(ex.getMessage().contains("Only orders in PENDING_PAYMENT or PAID state can be cancelled"));
    }
}
