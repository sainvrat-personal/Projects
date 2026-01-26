package com.example.orderservice.dto;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class OrderStatusTest {

    @Test
    void testAllOrderStatusValues() {
        // Test that all expected order statuses exist
        OrderStatus[] expectedStatuses = {
                OrderStatus.PENDING_PAYMENT,
                OrderStatus.PAID,
                OrderStatus.PROCESSING_IN_WAREHOUSE,
                OrderStatus.SHIPPED,
                OrderStatus.DELIVERED,
                OrderStatus.CANCELLED
        };

        OrderStatus[] actualStatuses = OrderStatus.values();
        assertEquals(expectedStatuses.length, actualStatuses.length);

        for (OrderStatus expected : expectedStatuses) {
            boolean found = false;
            for (OrderStatus actual : actualStatuses) {
                if (expected == actual) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "Expected status " + expected + " not found");
        }
    }

    @Test
    void testOrderStatusEnumValues() {
        // Test that enum values can be accessed and compared
        assertNotNull(OrderStatus.PENDING_PAYMENT);
        assertNotNull(OrderStatus.PAID);
        assertNotNull(OrderStatus.SHIPPED);
        assertNotNull(OrderStatus.DELIVERED);
        assertNotNull(OrderStatus.CANCELLED);

        // Test enum comparison
        assertEquals(OrderStatus.PENDING_PAYMENT, OrderStatus.PENDING_PAYMENT);
        assertNotEquals(OrderStatus.PENDING_PAYMENT, OrderStatus.PAID);

        // Test ordinal values
        assertTrue(OrderStatus.PENDING_PAYMENT.ordinal() >= 0);
        assertTrue(OrderStatus.PAID.ordinal() >= 0);
        assertTrue(OrderStatus.SHIPPED.ordinal() >= 0);
        assertTrue(OrderStatus.DELIVERED.ordinal() >= 0);
        assertTrue(OrderStatus.CANCELLED.ordinal() >= 0);
    }

    @Test
    void testOrderStatusToString() {
        assertEquals("PENDING_PAYMENT", OrderStatus.PENDING_PAYMENT.toString());
        assertEquals("PAID", OrderStatus.PAID.toString());
        assertEquals("SHIPPED", OrderStatus.SHIPPED.toString());
        assertEquals("DELIVERED", OrderStatus.DELIVERED.toString());
        assertEquals("CANCELLED", OrderStatus.CANCELLED.toString());
    }
}

class ReturnStatusTest {

    @Test
    void testAllReturnStatusValues() {
        // Test that all expected return statuses exist
        ReturnStatus[] expectedStatuses = {
                ReturnStatus.REQUESTED,
                ReturnStatus.APPROVED,
                ReturnStatus.REJECTED,
                ReturnStatus.IN_TRANSIT,
                ReturnStatus.RECEIVED,
                ReturnStatus.COMPLETED
        };

        ReturnStatus[] actualStatuses = ReturnStatus.values();
        assertEquals(expectedStatuses.length, actualStatuses.length);

        for (ReturnStatus expected : expectedStatuses) {
            boolean found = false;
            for (ReturnStatus actual : actualStatuses) {
                if (expected == actual) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "Expected status " + expected + " not found");
        }
    }

    @Test
    void testReturnStatusCanTransitionTo() {
        // Test valid transitions from REQUESTED
        assertTrue(ReturnStatus.REQUESTED.canTransitionTo(ReturnStatus.APPROVED));
        assertTrue(ReturnStatus.REQUESTED.canTransitionTo(ReturnStatus.REJECTED));
        assertFalse(ReturnStatus.REQUESTED.canTransitionTo(ReturnStatus.IN_TRANSIT));
        assertFalse(ReturnStatus.REQUESTED.canTransitionTo(ReturnStatus.RECEIVED));
        assertFalse(ReturnStatus.REQUESTED.canTransitionTo(ReturnStatus.COMPLETED));
        assertFalse(ReturnStatus.REQUESTED.canTransitionTo(ReturnStatus.REQUESTED));

        // Test valid transitions from APPROVED
        assertTrue(ReturnStatus.APPROVED.canTransitionTo(ReturnStatus.IN_TRANSIT));
        assertFalse(ReturnStatus.APPROVED.canTransitionTo(ReturnStatus.REQUESTED));
        assertFalse(ReturnStatus.APPROVED.canTransitionTo(ReturnStatus.REJECTED));
        assertFalse(ReturnStatus.APPROVED.canTransitionTo(ReturnStatus.RECEIVED));
        assertFalse(ReturnStatus.APPROVED.canTransitionTo(ReturnStatus.COMPLETED));
        assertFalse(ReturnStatus.APPROVED.canTransitionTo(ReturnStatus.APPROVED));

        // Test valid transitions from IN_TRANSIT
        assertTrue(ReturnStatus.IN_TRANSIT.canTransitionTo(ReturnStatus.RECEIVED));
        assertFalse(ReturnStatus.IN_TRANSIT.canTransitionTo(ReturnStatus.REQUESTED));
        assertFalse(ReturnStatus.IN_TRANSIT.canTransitionTo(ReturnStatus.APPROVED));
        assertFalse(ReturnStatus.IN_TRANSIT.canTransitionTo(ReturnStatus.REJECTED));
        assertFalse(ReturnStatus.IN_TRANSIT.canTransitionTo(ReturnStatus.COMPLETED));
        assertFalse(ReturnStatus.IN_TRANSIT.canTransitionTo(ReturnStatus.IN_TRANSIT));

        // Test valid transitions from RECEIVED
        assertTrue(ReturnStatus.RECEIVED.canTransitionTo(ReturnStatus.COMPLETED));
        assertFalse(ReturnStatus.RECEIVED.canTransitionTo(ReturnStatus.REQUESTED));
        assertFalse(ReturnStatus.RECEIVED.canTransitionTo(ReturnStatus.APPROVED));
        assertFalse(ReturnStatus.RECEIVED.canTransitionTo(ReturnStatus.REJECTED));
        assertFalse(ReturnStatus.RECEIVED.canTransitionTo(ReturnStatus.IN_TRANSIT));
        assertFalse(ReturnStatus.RECEIVED.canTransitionTo(ReturnStatus.RECEIVED));

        // Test no valid transitions from REJECTED
        assertFalse(ReturnStatus.REJECTED.canTransitionTo(ReturnStatus.REQUESTED));
        assertFalse(ReturnStatus.REJECTED.canTransitionTo(ReturnStatus.APPROVED));
        assertFalse(ReturnStatus.REJECTED.canTransitionTo(ReturnStatus.IN_TRANSIT));
        assertFalse(ReturnStatus.REJECTED.canTransitionTo(ReturnStatus.RECEIVED));
        assertFalse(ReturnStatus.REJECTED.canTransitionTo(ReturnStatus.COMPLETED));
        assertFalse(ReturnStatus.REJECTED.canTransitionTo(ReturnStatus.REJECTED));

        // Test no valid transitions from COMPLETED
        assertFalse(ReturnStatus.COMPLETED.canTransitionTo(ReturnStatus.REQUESTED));
        assertFalse(ReturnStatus.COMPLETED.canTransitionTo(ReturnStatus.APPROVED));
        assertFalse(ReturnStatus.COMPLETED.canTransitionTo(ReturnStatus.REJECTED));
        assertFalse(ReturnStatus.COMPLETED.canTransitionTo(ReturnStatus.IN_TRANSIT));
        assertFalse(ReturnStatus.COMPLETED.canTransitionTo(ReturnStatus.RECEIVED));
        assertFalse(ReturnStatus.COMPLETED.canTransitionTo(ReturnStatus.COMPLETED));
    }

    @Test
    void testReturnStatusToString() {
        assertEquals("REQUESTED", ReturnStatus.REQUESTED.toString());
        assertEquals("APPROVED", ReturnStatus.APPROVED.toString());
        assertEquals("REJECTED", ReturnStatus.REJECTED.toString());
        assertEquals("IN_TRANSIT", ReturnStatus.IN_TRANSIT.toString());
        assertEquals("RECEIVED", ReturnStatus.RECEIVED.toString());
        assertEquals("COMPLETED", ReturnStatus.COMPLETED.toString());
    }
}

class CreateOrderRequestTest {

    @Test
    void testCreateOrderRequestCreation() {
        // Given
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        CreateOrderRequest.Item item = new CreateOrderRequest.Item();
        item.setProductId(productId);
        item.setQuantity(2);
        item.setPrice(29.99);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(customerId);
        request.setItem(item);
        request.setShippingAddress("123 Test St");
        request.setEmail("test@gmail.com");

        // Then
        assertEquals(customerId, request.getCustomerId());
        assertEquals(item, request.getItem());
        assertEquals("123 Test St", request.getShippingAddress());
        assertEquals("test@gmail.com", request.getEmail());

        assertEquals(productId, request.getItem().getProductId());
        assertEquals(2, request.getItem().getQuantity());
        assertEquals(29.99, request.getItem().getPrice());
    }

    @Test
    void testCreateOrderRequestWithNullValues() {
        // Given
        CreateOrderRequest request = new CreateOrderRequest();

        // Then
        assertNull(request.getCustomerId());
        assertNull(request.getItem());
        assertNull(request.getShippingAddress());
        assertNull(request.getEmail());
    }

    @Test
    void testCreateOrderRequestEquality() {
        // Given
        UUID customerId = UUID.randomUUID();
        CreateOrderRequest.Item item = new CreateOrderRequest.Item();
        item.setProductId(UUID.randomUUID());
        item.setQuantity(1);
        item.setPrice(10.0);

        CreateOrderRequest request1 = new CreateOrderRequest();
        request1.setCustomerId(customerId);
        request1.setItem(item);
        request1.setShippingAddress("Address");
        request1.setEmail("test@gmail.com");

        CreateOrderRequest request2 = new CreateOrderRequest();
        request2.setCustomerId(customerId);
        request2.setItem(item);
        request2.setShippingAddress("Address");
        request2.setEmail("test@gmail.com");

        // Then
        assertEquals(request1, request2);
        assertEquals(request1.hashCode(), request2.hashCode());
    }

    @Test
    void testItemCreation() {
        // Given
        UUID productId = UUID.randomUUID();
        CreateOrderRequest.Item item = new CreateOrderRequest.Item();
        item.setProductId(productId);
        item.setQuantity(5);
        item.setPrice(99.99);

        // Then
        assertEquals(productId, item.getProductId());
        assertEquals(5, item.getQuantity());
        assertEquals(99.99, item.getPrice());
    }

    @Test
    void testItemWithZeroQuantity() {
        // Given
        CreateOrderRequest.Item item = new CreateOrderRequest.Item();
        item.setQuantity(0);
        item.setPrice(10.0);

        // Then
        assertEquals(0, item.getQuantity());
        assertEquals(10.0, item.getPrice());
    }

    @Test
    void testItemWithNegativeValues() {
        // Given
        CreateOrderRequest.Item item = new CreateOrderRequest.Item();
        item.setQuantity(-1);
        item.setPrice(-5.0);

        // Then
        assertEquals(-1, item.getQuantity());
        assertEquals(-5.0, item.getPrice());
    }
}

class RefundRequestTest {

    @Test
    void testRefundRequestCreation() {
        // Given
        RefundRequest request = new RefundRequest();
        request.setReturnId("return-123");
        request.setOrderId("order-456");
        request.setTransactionId("trans-789");
        request.setAmount(99.99);
        request.setCustomerEmail("test@gmail.com");
        request.setReason("Product damaged");

        // Then
        assertEquals("return-123", request.getReturnId());
        assertEquals("order-456", request.getOrderId());
        assertEquals("trans-789", request.getTransactionId());
        assertEquals(99.99, request.getAmount());
        assertEquals("test@gmail.com", request.getCustomerEmail());
        assertEquals("Product damaged", request.getReason());
    }

    @Test
    void testRefundRequestWithNullValues() {
        // Given
        RefundRequest request = new RefundRequest();

        // Then
        assertNull(request.getReturnId());
        assertNull(request.getOrderId());
        assertNull(request.getTransactionId());
        assertNull(request.getAmount());
        assertNull(request.getCustomerEmail());
        assertNull(request.getReason());
    }

    @Test
    void testRefundRequestWithZeroAmount() {
        // Given
        RefundRequest request = new RefundRequest();
        request.setAmount(0.0);

        // Then
        assertEquals(0.0, request.getAmount());
    }

    @Test
    void testRefundRequestWithNegativeAmount() {
        // Given
        RefundRequest request = new RefundRequest();
        request.setAmount(-50.0);

        // Then
        assertEquals(-50.0, request.getAmount());
    }
}

// ErrorResponse is tested in a dedicated test class: ErrorResponseTest