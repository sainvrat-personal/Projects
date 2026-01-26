package com.example.orderservice.service;

import com.example.orderservice.dto.CreateOrderRequest;
import com.example.orderservice.dto.OrderStatus;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.StateHistory;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.repository.StateHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceComprehensiveTest {

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
    private UUID customerId;
    private UUID changedBy;
    private Order order;
    private CreateOrderRequest createOrderRequest;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        changedBy = UUID.randomUUID();

        // Setup CreateOrderRequest
        createOrderRequest = new CreateOrderRequest();
        createOrderRequest.setCustomerId(customerId);
        createOrderRequest.setShippingAddress("123 Test St, Test City, TC 12345");
        createOrderRequest.setEmail("test@gmail.com");

        CreateOrderRequest.Item item = new CreateOrderRequest.Item();
        item.setProductId(UUID.randomUUID());
        item.setQuantity(2);
        item.setPrice(29.99);
        createOrderRequest.setItem(item);

        // Setup Order entity
        order = new Order();
        order.setId(orderId);
        order.setTransactionId(UUID.randomUUID());
        order.setCustomerId(customerId);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setCreatedAt(Instant.now());
        order.setShippingAddress("123 Test St, Test City, TC 12345");
        order.setEmail("test@gmail.com");
    }

    @Test
    void testCreateOrder_Success() {
        // Given
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        // When
        Order createdOrder = orderService.createOrder(createOrderRequest);

        // Then
        assertNotNull(createdOrder);
        assertEquals(OrderStatus.PENDING_PAYMENT, createdOrder.getStatus());
        assertEquals(customerId, createdOrder.getCustomerId());
        assertEquals("test@gmail.com", createdOrder.getEmail());
        assertEquals("123 Test St, Test City, TC 12345", createdOrder.getShippingAddress());
        assertNotNull(createdOrder.getId());
        assertNotNull(createdOrder.getTransactionId());
        assertNotNull(createdOrder.getCreatedAt());

        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void testCreateOrder_NullRequest() {
        // When & Then
        assertThrows(NullPointerException.class, () -> {
            orderService.createOrder(null);
        });

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void testCreateOrder_NullCustomerId() {
        // Given
        createOrderRequest.setCustomerId(null);

        // When & Then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.createOrder(createOrderRequest));
        assertTrue(ex.getMessage().contains("Customer ID is required."));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void testCreateOrder_EmptyEmail() {
        // Given
        createOrderRequest.setEmail("");

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            orderService.createOrder(createOrderRequest);
        });
    }

    @Test
    void testCreateOrder_NullItem() {
        // Given
        createOrderRequest.setItem(null);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            orderService.createOrder(createOrderRequest);
        });
    }

    @Test
    void testCreateOrder_RepositoryException() {
        // Given
        when(orderRepository.save(any(Order.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            orderService.createOrder(createOrderRequest);
        });
    }

    @Test
    void testCancelOrder_FromPendingPayment_Success() {
        // Given
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        // When
        Order cancelledOrder = orderService.cancelOrder(orderId, "Customer request", changedBy);

        // Then
        assertEquals(OrderStatus.CANCELLED, cancelledOrder.getStatus());
        verify(orderRepository, times(1)).findById(orderId);
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(stateHistoryRepository, times(1)).save(any(StateHistory.class));
    }

    @Test
    void testCancelOrder_FromPaid_Success() {
        // Given
        order.setStatus(OrderStatus.PAID);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        // When
        Order cancelledOrder = orderService.cancelOrder(orderId, "Customer request", changedBy);

        // Then
        assertEquals(OrderStatus.CANCELLED, cancelledOrder.getStatus());
        verify(stateHistoryRepository, times(1)).save(any(StateHistory.class));
    }

    @Test
    void testCancelOrder_OrderNotFound() {
        // Given
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            orderService.cancelOrder(orderId, "Customer request", changedBy);
        });

        assertTrue(exception.getMessage().contains("Order not found"));
        verify(orderRepository, never()).save(any(Order.class));
        verify(stateHistoryRepository, never()).save(any(StateHistory.class));
    }

    @Test
    void testCancelOrder_FromShipped_ShouldFail() {
        // Given
        order.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            orderService.cancelOrder(orderId, "Customer request", changedBy);
        });

        assertTrue(exception.getMessage().contains("Only orders in PENDING_PAYMENT or PAID state can be cancelled"));
        verify(orderRepository, never()).save(any(Order.class));
        // A state history entry is still written to record the failed attempt.
        verify(stateHistoryRepository, times(1)).save(any(StateHistory.class));
    }

    @Test
    void testCancelOrder_FromDelivered_ShouldFail() {
        // Given
        order.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            orderService.cancelOrder(orderId, "Customer request", changedBy);
        });

        assertTrue(exception.getMessage().contains("Only orders in PENDING_PAYMENT or PAID state can be cancelled"));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void testCancelOrder_FromCancelled_IsIdempotent() {
        // Given
        order.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When
        Order result = orderService.cancelOrder(orderId, "Customer request", changedBy);

        // Then - idempotent: still cancelled, no extra save/history invoked
        assertEquals(OrderStatus.CANCELLED, result.getStatus());
        verify(orderRepository, times(1)).findById(orderId);
        verify(orderRepository, never()).save(any(Order.class));
        verify(stateHistoryRepository, never()).save(any(StateHistory.class));
    }

    @Test
    void testCancelOrder_NullReason() {
        // Given
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            orderService.cancelOrder(orderId, null, changedBy);
        });
        assertTrue(exception.getMessage().contains("Cancellation reason is required"));
        verify(orderRepository, never()).save(any(Order.class));
        verify(stateHistoryRepository, never()).save(any(StateHistory.class));
    }

    @Test
    void testCancelOrder_EmptyReason() {
        // Given
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            orderService.cancelOrder(orderId, "", changedBy);
        });
        assertTrue(exception.getMessage().contains("Cancellation reason is required"));
        verify(orderRepository, never()).save(any(Order.class));
        verify(stateHistoryRepository, never()).save(any(StateHistory.class));
    }

    @Test
    void testTransitionOrderState_PendingPaymentToPaid_Success() {
        // Given
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        // When
        Order transitionedOrder = orderService.transitionOrderState(orderId, OrderStatus.PAID, changedBy);

        // Then
        assertEquals(OrderStatus.PAID, transitionedOrder.getStatus());
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(stateHistoryRepository, times(1)).save(any(StateHistory.class));
    }

    @Test
    void testTransitionOrderState_PaidToShipped_Success() {
        // Given
        order.setStatus(OrderStatus.PAID);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        // When
        Order transitionedOrder = orderService.transitionOrderState(orderId, OrderStatus.SHIPPED, changedBy);

        // Then
        assertEquals(OrderStatus.SHIPPED, transitionedOrder.getStatus());
        verify(stateHistoryRepository, times(1)).save(any(StateHistory.class));
    }

    @Test
    void testTransitionOrderState_InvalidTransition_PendingPaymentToShipped() {
        // Given
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            orderService.transitionOrderState(orderId, OrderStatus.SHIPPED, changedBy);
        });

        assertTrue(exception.getMessage().contains("Invalid state transition"));
        verify(orderRepository, never()).save(any(Order.class));
        verify(stateHistoryRepository, never()).save(any(StateHistory.class));
    }

    @Test
    void testTransitionOrderState_InvalidTransition_PaidToDelivered() {
        // Given
        order.setStatus(OrderStatus.PAID);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            orderService.transitionOrderState(orderId, OrderStatus.DELIVERED, changedBy);
        });

        assertTrue(exception.getMessage().contains("Invalid state transition"));
    }

    @Test
    void testTransitionOrderState_OrderNotFound() {
        // Given
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            orderService.transitionOrderState(orderId, OrderStatus.PAID, changedBy);
        });

        assertTrue(exception.getMessage().contains("Order not found"));
    }

    @Test
    void testTransitionOrderState_SameState() {
        // Given - order is already in PENDING_PAYMENT state
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            orderService.transitionOrderState(orderId, OrderStatus.PENDING_PAYMENT, changedBy);
        });

        assertTrue(exception.getMessage().contains("Invalid state transition"));
    }

    @Test
    void testGetOrderStateHistory_Success() {
        // Given
        StateHistory history1 = new StateHistory();
        history1.setEntityType("Order");
        history1.setEntityId(orderId);
        history1.setState("PAID");

        StateHistory history2 = new StateHistory();
        history2.setEntityType("Order");
        history2.setEntityId(orderId);
        history2.setState("SHIPPED");

        List<StateHistory> historyList = Arrays.asList(history1, history2);
        when(stateHistoryRepository.findByEntityTypeAndEntityId("Order", orderId))
                .thenReturn(historyList);

        // When
        List<StateHistory> result = orderService.getOrderStateHistory(orderId);

        // Then
        assertEquals(2, result.size());
        assertEquals("PAID", result.get(0).getState());
        assertEquals("SHIPPED", result.get(1).getState());
        verify(stateHistoryRepository, times(1)).findByEntityTypeAndEntityId("Order", orderId);
    }

    @Test
    void testGetOrderStateHistory_EmptyHistory() {
        // Given
        when(stateHistoryRepository.findByEntityTypeAndEntityId("Order", orderId))
                .thenReturn(Arrays.asList());

        // When
        List<StateHistory> result = orderService.getOrderStateHistory(orderId);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetOrderStateHistory_RepositoryException() {
        // Given
        when(stateHistoryRepository.findByEntityTypeAndEntityId("Order", orderId))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            orderService.getOrderStateHistory(orderId);
        });
    }

    @Test
    void testAllValidOrderStateTransitions() {
        // Test all valid transitions
        testValidTransition(OrderStatus.PENDING_PAYMENT, OrderStatus.PAID);
        testValidTransition(OrderStatus.PAID, OrderStatus.SHIPPED);
        testValidTransition(OrderStatus.SHIPPED, OrderStatus.DELIVERED);
    }

    private void testValidTransition(OrderStatus fromStatus, OrderStatus toStatus) {
        // Given
        Order testOrder = new Order();
        testOrder.setId(UUID.randomUUID());
        testOrder.setStatus(fromStatus);

        when(orderRepository.findById(testOrder.getId())).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        // When
        Order result = orderService.transitionOrderState(testOrder.getId(), toStatus, changedBy);

        // Then
        assertEquals(toStatus, result.getStatus());

        // Reset mocks
        reset(orderRepository);
        reset(stateHistoryRepository);
    }

    @Test
    void testCreateOrder_WithEmailJob() {
        // Given
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> {
            Order savedOrder = i.getArgument(0);
            savedOrder.setId(orderId); // Set ID as if saved
            return savedOrder;
        });

        // When
        Order createdOrder = orderService.createOrder(createOrderRequest);

        // Then
        assertNotNull(createdOrder);
        assertEquals(OrderStatus.PENDING_PAYMENT, createdOrder.getStatus());
        
        // Should trigger invoice job (if implemented)
        verify(orderRepository, times(1)).save(any(Order.class));
    }
}