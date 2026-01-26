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

class OrderServiceStateTransitionTestUpdated {
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

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        orderId = UUID.randomUUID();
        changedBy = UUID.randomUUID();
        order = new Order();
        order.setId(orderId);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
    }

    @Test
    void testValidStateTransition_PendingPaymentToPaid() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        
        Order updated = orderService.transitionOrderState(orderId, OrderStatus.PAID, changedBy);
        
        assertEquals(OrderStatus.PAID, updated.getStatus());
        verify(stateHistoryRepository, times(1)).save(any());
    }
    
    @Test
    void testValidStateTransition_PaidToProcessingInWarehouse() {
        order.setStatus(OrderStatus.PAID);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        
        Order updated = orderService.transitionOrderState(orderId, OrderStatus.PROCESSING_IN_WAREHOUSE, changedBy);
        
        assertEquals(OrderStatus.PROCESSING_IN_WAREHOUSE, updated.getStatus());
        verify(stateHistoryRepository, times(1)).save(any());
    }
    
    @Test
    void testValidStateTransition_ProcessingToShipped() {
        order.setStatus(OrderStatus.PROCESSING_IN_WAREHOUSE);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        
        Order updated = orderService.transitionOrderState(orderId, OrderStatus.SHIPPED, changedBy);
        
        assertEquals(OrderStatus.SHIPPED, updated.getStatus());
        verify(stateHistoryRepository, times(1)).save(any());
        // Verify invoice job is scheduled (idempotently)
        verify(jobScheduler, times(1)).scheduleInvoiceJob(eq(orderId));
    }
    
    @Test
    void testValidStateTransition_ShippedToDelivered() {
        order.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        
        Order updated = orderService.transitionOrderState(orderId, OrderStatus.DELIVERED, changedBy);
        
        assertEquals(OrderStatus.DELIVERED, updated.getStatus());
        verify(stateHistoryRepository, times(1)).save(any());
    }
    
    @Test
    void testValidStateTransition_PendingPaymentToCancelled() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        
        Order updated = orderService.transitionOrderState(orderId, OrderStatus.CANCELLED, changedBy);
        
        assertEquals(OrderStatus.CANCELLED, updated.getStatus());
        verify(stateHistoryRepository, times(1)).save(any());
    }
    
    @Test
    void testValidStateTransition_PaidToCancelled() {
        order.setStatus(OrderStatus.PAID);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        
        Order updated = orderService.transitionOrderState(orderId, OrderStatus.CANCELLED, changedBy);
        
        assertEquals(OrderStatus.CANCELLED, updated.getStatus());
        verify(stateHistoryRepository, times(1)).save(any());
    }

    @Test
    void testInvalidStateTransition_PendingPaymentToShipped() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        
        IllegalStateException exception = assertThrows(
            IllegalStateException.class, 
            () -> orderService.transitionOrderState(orderId, OrderStatus.SHIPPED, changedBy)
        );
        
        assertTrue(exception.getMessage().contains("Invalid state transition"));
        verify(orderRepository, never()).save(any());
    }
    
    @Test
    void testInvalidStateTransition_DeliveredToPaid() {
        order.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        
        IllegalStateException exception = assertThrows(
            IllegalStateException.class, 
            () -> orderService.transitionOrderState(orderId, OrderStatus.PAID, changedBy)
        );
        
        assertTrue(exception.getMessage().contains("Invalid state transition"));
        verify(orderRepository, never()).save(any());
    }
    
    @Test
    void testInvalidStateTransition_ShippedToCancelled() {
        order.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        
        IllegalStateException exception = assertThrows(
            IllegalStateException.class, 
            () -> orderService.transitionOrderState(orderId, OrderStatus.CANCELLED, changedBy)
        );
        
        assertTrue(exception.getMessage().contains("Invalid state transition"));
        verify(orderRepository, never()).save(any());
    }
    
    @Test
    void testOrderNotFound() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());
        
        assertThrows(
            IllegalArgumentException.class, 
            () -> orderService.transitionOrderState(orderId, OrderStatus.PAID, changedBy)
        );
    }
}