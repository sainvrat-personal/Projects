package com.example.orderservice.controller;

import com.example.orderservice.dto.CreateOrderRequest;
import com.example.orderservice.dto.OrderCancellationRequest;
import com.example.orderservice.dto.OrderStateTransitionRequest;
import com.example.orderservice.dto.OrderStatus;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.StateHistory;
import com.example.orderservice.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Autowired
    private ObjectMapper objectMapper;

    private Order order;
    private CreateOrderRequest createOrderRequest;
    private CreateOrderRequest.Item item1;
    private UUID customerId;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        orderId = UUID.randomUUID();

        item1 = new CreateOrderRequest.Item();
        item1.setProductId(UUID.randomUUID());
        item1.setQuantity(2);
        item1.setPrice(29.99);

        // Setup CreateOrderRequest
        createOrderRequest = new CreateOrderRequest();
        createOrderRequest.setCustomerId(customerId);
        createOrderRequest.setItem(item1);
        createOrderRequest.setShippingAddress("123 Test St, Test City, TC 12345");
        createOrderRequest.setEmail("test@gmail.com");
        createOrderRequest.setTransactionId(UUID.randomUUID());

        // Setup Order
        order = new Order();
        order.setId(orderId);
        order.setCustomerId(customerId);
        order.setProductId(item1.getProductId());
        order.setQuantity(item1.getQuantity());
        order.setPrice(item1.getPrice());
        order.setShippingAddress(createOrderRequest.getShippingAddress());
        order.setEmail(createOrderRequest.getEmail());
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setCreatedAt(Instant.now());
    }

    @Test
    void testPlaceOrder_Success() throws Exception {
        // Given
        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(order);

        // When & Then
        mockMvc.perform(post("/orders/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void testPlaceOrder_NonGmailEmail_Warning() throws Exception {
        // Given
        // Valid email format but non-Gmail to trigger soft warning branch
        createOrderRequest.setEmail("user@yahoo.com");
        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(order);

        // When & Then
        mockMvc.perform(post("/orders/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));
    }

    @Test
    void testPlaceOrder_EmptyEmail() throws Exception {
        createOrderRequest.setEmail("");
        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(order);

        mockMvc.perform(post("/orders/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));
    }

    @Test
    void testPlaceOrder_ServiceException() throws Exception {
        // Given
        when(orderService.createOrder(any(CreateOrderRequest.class)))
            .thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        mockMvc.perform(post("/orders/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testPlaceOrder_InvalidPayload_NullCustomerId() throws Exception {
        createOrderRequest.setCustomerId(null);
        when(orderService.createOrder(any(CreateOrderRequest.class)))
            .thenThrow(new IllegalArgumentException("Customer ID is required"));

        mockMvc.perform(post("/orders/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testCancelOrder_Success() throws Exception {
        // Given
        OrderCancellationRequest cancelRequest = new OrderCancellationRequest();
        cancelRequest.setReason("Customer request");
        cancelRequest.setChangedBy(UUID.randomUUID());

        Order cancelledOrder = new Order();
        cancelledOrder.setId(orderId);
        cancelledOrder.setStatus(OrderStatus.CANCELLED);

        when(orderService.cancelOrder(eq(orderId), eq("Customer request"), any(UUID.class)))
            .thenReturn(cancelledOrder);

        // When & Then
        mockMvc.perform(patch("/orders/{orderId}/cancel", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cancelRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void testCancelOrder_OrderNotFound() throws Exception {
        // Given
        OrderCancellationRequest cancelRequest = new OrderCancellationRequest();
        cancelRequest.setReason("Customer request");
        cancelRequest.setChangedBy(UUID.randomUUID());

        when(orderService.cancelOrder(eq(orderId), eq("Customer request"), any(UUID.class)))
            .thenThrow(new IllegalArgumentException("Order not found: " + orderId));

        // When & Then
        mockMvc.perform(patch("/orders/{orderId}/cancel", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cancelRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testCancelOrder_InvalidState() throws Exception {
        // Given
        OrderCancellationRequest cancelRequest = new OrderCancellationRequest();
        cancelRequest.setReason("Customer request");
        cancelRequest.setChangedBy(UUID.randomUUID());

        when(orderService.cancelOrder(eq(orderId), eq("Customer request"), any(UUID.class)))
            .thenThrow(new IllegalStateException("Cannot cancel order in current state"));

        // When & Then
        mockMvc.perform(patch("/orders/{orderId}/cancel", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cancelRequest)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testCancelOrder_ValidationError_MissingChangedBy() throws Exception {
        OrderCancellationRequest cancelRequest = new OrderCancellationRequest();
        cancelRequest.setReason("Customer request");
        cancelRequest.setChangedBy(null);

        mockMvc.perform(patch("/orders/{orderId}/cancel", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cancelRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("changedBy is required for auditing."));
    }

    @Test
    void testCancelOrder_ValidationError_MissingReason() throws Exception {
        OrderCancellationRequest cancelRequest = new OrderCancellationRequest();
        cancelRequest.setReason("  ");
        cancelRequest.setChangedBy(UUID.randomUUID());

        mockMvc.perform(patch("/orders/{orderId}/cancel", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cancelRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Cancellation reason is required."));
    }

    @Test
    void testTransitionOrderState_Success() throws Exception {
        // Given
        OrderStateTransitionRequest transitionRequest = new OrderStateTransitionRequest();
        transitionRequest.setNewState(OrderStatus.PAID);
        transitionRequest.setChangedBy(UUID.randomUUID());

        Order updatedOrder = new Order();
        updatedOrder.setId(orderId);
        updatedOrder.setStatus(OrderStatus.PAID);

        when(orderService.transitionOrderState(eq(orderId), eq(OrderStatus.PAID), any(UUID.class)))
            .thenReturn(updatedOrder);

        // When & Then
        mockMvc.perform(patch("/orders/{orderId}/state", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transitionRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void testTransitionOrderState_InvalidTransition() throws Exception {
        // Given
        OrderStateTransitionRequest transitionRequest = new OrderStateTransitionRequest();
        transitionRequest.setNewState(OrderStatus.DELIVERED);
        transitionRequest.setChangedBy(UUID.randomUUID());

        when(orderService.transitionOrderState(eq(orderId), eq(OrderStatus.DELIVERED), any(UUID.class)))
            .thenThrow(new IllegalStateException("Invalid state transition"));

        // When & Then
        mockMvc.perform(patch("/orders/{orderId}/state", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transitionRequest)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testTransitionOrderState_ValidationError_MissingNewState() throws Exception {
        OrderStateTransitionRequest transitionRequest = new OrderStateTransitionRequest();
        transitionRequest.setNewState(null);
        transitionRequest.setChangedBy(UUID.randomUUID());

        mockMvc.perform(patch("/orders/{orderId}/state", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transitionRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("New state is required."));
    }

    @Test
    void testTransitionOrderState_ValidationError_MissingChangedBy() throws Exception {
        OrderStateTransitionRequest transitionRequest = new OrderStateTransitionRequest();
        transitionRequest.setNewState(OrderStatus.PAID);
        transitionRequest.setChangedBy(null);

        mockMvc.perform(patch("/orders/{orderId}/state", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transitionRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("changedBy is required for auditing."));
    }

    @Test
    void testGetOrderStateHistory_Success() throws Exception {
        // Given
        StateHistory history1 = new StateHistory();
        history1.setEntityType("Order");
        history1.setEntityId(orderId);
        history1.setState(OrderStatus.PAID.toString());
        history1.setTimestamp(Instant.now());

        List<StateHistory> historyList = Arrays.asList(history1);
        when(orderService.getOrderStateHistory(orderId)).thenReturn(historyList);

        // When & Then
        mockMvc.perform(get("/orders/{orderId}/state-history", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].entityType").value("Order"))
                .andExpect(jsonPath("$[0].state").value("PAID"));
    }

    @Test
    void testGetOrderStateHistory_OrderNotFound() throws Exception {
        // Given
        when(orderService.getOrderStateHistory(orderId))
            .thenThrow(new IllegalArgumentException("Order not found: " + orderId));

        // When & Then
        mockMvc.perform(get("/orders/{orderId}/state-history", orderId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testGetOrderStateHistory_ServiceException() throws Exception {
        // Given
        when(orderService.getOrderStateHistory(orderId))
            .thenThrow(new RuntimeException("Database error"));

        // When & Then
        mockMvc.perform(get("/orders/{orderId}/state-history", orderId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testGetOrderStateHistory_EmptyHistory() throws Exception {
        // Given
        when(orderService.getOrderStateHistory(orderId)).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/orders/{orderId}/state-history", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}