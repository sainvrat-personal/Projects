package com.example.orderservice.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CreateOrderResponseTest {

    @Test
    void noArgsConstructorAndSetters_populateFieldsCorrectly() {
        UUID orderId = UUID.randomUUID();
        OrderStatus status = OrderStatus.PAID;
        Instant createdAt = Instant.now();

        CreateOrderResponse response = new CreateOrderResponse();
        response.setOrderId(orderId);
        response.setStatus(status);
        response.setCreatedAt(createdAt);

        assertEquals(orderId, response.getOrderId());
        assertEquals(status, response.getStatus());
        assertEquals(createdAt, response.getCreatedAt());
    }

    @Test
    void allArgsConstructor_populatesAllFields() {
        UUID orderId = UUID.randomUUID();
        OrderStatus status = OrderStatus.PENDING_PAYMENT;
        Instant createdAt = Instant.now();

        CreateOrderResponse response = new CreateOrderResponse(orderId, status, createdAt);

        assertEquals(orderId, response.getOrderId());
        assertEquals(status, response.getStatus());
        assertEquals(createdAt, response.getCreatedAt());
    }

    @Test
    void gettersReturnNullByDefault_whenUsingNoArgsConstructor() {
        CreateOrderResponse response = new CreateOrderResponse();

        assertNull(response.getOrderId());
        assertNull(response.getStatus());
        assertNull(response.getCreatedAt());
    }

    @Test
    void equalsAndHashCode_coverPositiveAndNegativeCases() {
        UUID orderId = UUID.randomUUID();
        Instant createdAt = Instant.now();

        CreateOrderResponse r1 = new CreateOrderResponse(orderId, OrderStatus.SHIPPED, createdAt);
        CreateOrderResponse r2 = new CreateOrderResponse(orderId, OrderStatus.SHIPPED, createdAt);
        CreateOrderResponse rDifferent = new CreateOrderResponse(orderId, OrderStatus.DELIVERED, createdAt);

        // same instance
        assertEquals(r1, r1);

        // equal values
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());

        // different content
        assertNotEquals(r1, rDifferent);

        // null and different type
        assertNotEquals(r1, null);
        assertNotEquals(r1, new Object());
    }

    @Test
    void toString_containsKeyInformationAndDoesNotThrow() {
        UUID orderId = UUID.randomUUID();
        Instant createdAt = Instant.now();

        CreateOrderResponse response = new CreateOrderResponse(orderId, OrderStatus.PROCESSING_IN_WAREHOUSE, createdAt);

        String s = response.toString();
        assertNotNull(s);
        assertTrue(s.contains(orderId.toString()));
        assertTrue(s.contains("PROCESSING_IN_WAREHOUSE"));
    }
}

