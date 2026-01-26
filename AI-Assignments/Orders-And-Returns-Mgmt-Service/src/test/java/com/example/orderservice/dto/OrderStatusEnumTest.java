package com.example.orderservice.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderStatusEnumTest {

    @Test
    void values_containsAllExpectedStatusesInOrder() {
        OrderStatus[] expected = {
                OrderStatus.PENDING_PAYMENT,
                OrderStatus.PAID,
                OrderStatus.PROCESSING_IN_WAREHOUSE,
                OrderStatus.SHIPPED,
                OrderStatus.DELIVERED,
                OrderStatus.CANCELLED
        };

        OrderStatus[] actual = OrderStatus.values();

        assertArrayEquals(expected, actual);
    }

    @Test
    void valueOf_returnsCorrectEnumForEachName() {
        assertEquals(OrderStatus.PENDING_PAYMENT, OrderStatus.valueOf("PENDING_PAYMENT"));
        assertEquals(OrderStatus.PAID, OrderStatus.valueOf("PAID"));
        assertEquals(OrderStatus.PROCESSING_IN_WAREHOUSE, OrderStatus.valueOf("PROCESSING_IN_WAREHOUSE"));
        assertEquals(OrderStatus.SHIPPED, OrderStatus.valueOf("SHIPPED"));
        assertEquals(OrderStatus.DELIVERED, OrderStatus.valueOf("DELIVERED"));
        assertEquals(OrderStatus.CANCELLED, OrderStatus.valueOf("CANCELLED"));
    }

    @Test
    void valueOf_throwsExceptionForInvalidName() {
        assertThrows(IllegalArgumentException.class, () -> OrderStatus.valueOf("UNKNOWN_STATUS"));
    }

    @Test
    void toString_returnsNameOfEnumConstant() {
        for (OrderStatus status : OrderStatus.values()) {
            assertEquals(status.name(), status.toString());
        }
    }

    @Test
    void ordinal_isNonNegativeAndUnique() {
        boolean[] seen = new boolean[OrderStatus.values().length];

        for (OrderStatus status : OrderStatus.values()) {
            int ordinal = status.ordinal();
            assertTrue(ordinal >= 0);
            assertFalse(seen[ordinal], "Duplicate ordinal " + ordinal + " for " + status);
            seen[ordinal] = true;
        }
    }
}

