package com.example.orderservice.entity;

import com.example.orderservice.dto.OrderStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    @Test
    void noArgsConstructorAndSetters_populateFieldsCorrectly() {
        Order order = new Order();

        UUID id = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        OrderStatus status = OrderStatus.PAID;
        String shippingAddress = "123 Test Street";
        String email = "customer@example.com";
        String failureReason = "None";
        UUID productId = UUID.randomUUID();
        int quantity = 5;
        double price = 19.99;
        Instant createdAt = Instant.now().minusSeconds(60);
        Instant updatedAt = Instant.now();
        Long version = 1L;

        order.setId(id);
        order.setTransactionId(transactionId);
        order.setCustomerId(customerId);
        order.setStatus(status);
        order.setShippingAddress(shippingAddress);
        order.setEmail(email);
        order.setFailureReason(failureReason);
        order.setProductId(productId);
        order.setQuantity(quantity);
        order.setPrice(price);
        order.setCreatedAt(createdAt);
        order.setUpdatedAt(updatedAt);
        order.setVersion(version);

        assertEquals(id, order.getId());
        assertEquals(transactionId, order.getTransactionId());
        assertEquals(customerId, order.getCustomerId());
        assertEquals(status, order.getStatus());
        assertEquals(shippingAddress, order.getShippingAddress());
        assertEquals(email, order.getEmail());
        assertEquals(failureReason, order.getFailureReason());
        assertEquals(productId, order.getProductId());
        assertEquals(quantity, order.getQuantity());
        assertEquals(price, order.getPrice());
        assertEquals(createdAt, order.getCreatedAt());
        assertEquals(updatedAt, order.getUpdatedAt());
        assertEquals(version, order.getVersion());
    }

    @Test
    void allArgsConstructor_populatesAllFields() {
        UUID id = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        OrderStatus status = OrderStatus.SHIPPED;
        String shippingAddress = "456 Another Street";
        String email = "user@example.com";
        String failureReason = "Delayed";
        UUID productId = UUID.randomUUID();
        int quantity = 2;
        double price = 49.5;
        Instant createdAt = Instant.now().minusSeconds(120);
        Instant updatedAt = Instant.now().minusSeconds(30);
        Long version = 3L;

        Order order = new Order(
                id,
                transactionId,
                customerId,
                status,
                shippingAddress,
                email,
                failureReason,
                productId,
                quantity,
                price,
                createdAt,
                updatedAt,
                version
        );

        assertEquals(id, order.getId());
        assertEquals(transactionId, order.getTransactionId());
        assertEquals(customerId, order.getCustomerId());
        assertEquals(status, order.getStatus());
        assertEquals(shippingAddress, order.getShippingAddress());
        assertEquals(email, order.getEmail());
        assertEquals(failureReason, order.getFailureReason());
        assertEquals(productId, order.getProductId());
        assertEquals(quantity, order.getQuantity());
        assertEquals(price, order.getPrice());
        assertEquals(createdAt, order.getCreatedAt());
        assertEquals(updatedAt, order.getUpdatedAt());
        assertEquals(version, order.getVersion());
    }

    @Test
    void defaultFieldValues_areInitialized() {
        Order order = new Order();

        assertNull(order.getId());
        assertNull(order.getTransactionId());
        assertNull(order.getCustomerId());
        assertNull(order.getStatus());
        assertNull(order.getShippingAddress());
        assertNull(order.getEmail());
        assertNull(order.getFailureReason());
        assertNull(order.getProductId());
        assertEquals(0, order.getQuantity());
        assertEquals(0.0, order.getPrice());
        assertNotNull(order.getCreatedAt());
        assertNotNull(order.getUpdatedAt());
        assertNull(order.getVersion());
    }

    @Test
    void equalsAndHashCode_coverPositiveAndNegativeCases() {
        UUID id = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Instant createdAt = Instant.now().minusSeconds(100);
        Instant updatedAt = Instant.now().minusSeconds(50);

        Order o1 = new Order(
                id,
                transactionId,
                customerId,
                OrderStatus.PENDING_PAYMENT,
                "Addr",
                "a@example.com",
                null,
                productId,
                1,
                10.0,
                createdAt,
                updatedAt,
                1L
        );

        Order o2 = new Order(
                id,
                transactionId,
                customerId,
                OrderStatus.PENDING_PAYMENT,
                "Addr",
                "a@example.com",
                null,
                productId,
                1,
                10.0,
                createdAt,
                updatedAt,
                1L
        );

        Order oDifferent = new Order(
                id,
                transactionId,
                customerId,
                OrderStatus.CANCELLED,
                "Other",
                "b@example.com",
                "fail",
                productId,
                2,
                5.0,
                createdAt,
                updatedAt,
                2L
        );

        // same instance
        assertEquals(o1, o1);

        // equal values
        assertEquals(o1, o2);
        assertEquals(o1.hashCode(), o2.hashCode());

        // different content
        assertNotEquals(o1, oDifferent);

        // null and different type
        assertNotEquals(o1, null);
        assertNotEquals(o1, new Object());
    }

    @Test
    void toString_containsKeyInformationAndDoesNotThrow() {
        Order order = new Order();
        UUID id = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        order.setId(id);
        order.setCustomerId(customerId);
        order.setStatus(OrderStatus.PAID);

        String s = order.toString();
        assertNotNull(s);
        assertTrue(s.contains(id.toString()));
        assertTrue(s.contains(customerId.toString()));
        assertTrue(s.contains(OrderStatus.PAID.name()));
    }
}

