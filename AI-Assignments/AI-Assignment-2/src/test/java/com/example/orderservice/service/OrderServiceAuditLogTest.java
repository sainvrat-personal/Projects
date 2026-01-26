package com.example.orderservice.service;

import com.example.orderservice.dto.OrderStatus;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.StateHistory;
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

class OrderServiceAuditLogTest {
    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private StateHistoryRepository stateHistoryRepository;
    
    @Mock
    private InvoiceJobService invoiceJobService;
    
    @Mock
    private EmailService emailService;
    
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
    void testAuditLogOnStateChange() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        when(stateHistoryRepository.save(any(StateHistory.class))).thenAnswer(i -> i.getArgument(0));
        orderService.transitionOrderState(orderId, OrderStatus.PAID, changedBy);
        verify(stateHistoryRepository, times(1)).save(any(StateHistory.class));
    }

    @Test
    void testAuditLogOnCancellation() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        when(stateHistoryRepository.save(any(StateHistory.class))).thenAnswer(i -> i.getArgument(0));
        orderService.cancelOrder(orderId, cancellationReason, changedBy);
        verify(stateHistoryRepository, times(1)).save(any(StateHistory.class));
    }
}
