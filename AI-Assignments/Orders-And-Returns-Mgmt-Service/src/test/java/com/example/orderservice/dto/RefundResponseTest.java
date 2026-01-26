package com.example.orderservice.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RefundResponseTest {

    @Test
    void noArgsConstructorAndSetters_populateFieldsCorrectly() {
        RefundResponse response = new RefundResponse();
        response.setSuccess(true);
        response.setMessage("Refund processed successfully");
        response.setTransactionId("txn-123");
        response.setRefundId("refund-456");
        response.setAmount(99.99);
        response.setStatus("COMPLETED");
        response.setTimestamp("2026-01-26T10:15:30Z");

        assertTrue(response.isSuccess());
        assertEquals("Refund processed successfully", response.getMessage());
        assertEquals("txn-123", response.getTransactionId());
        assertEquals("refund-456", response.getRefundId());
        assertEquals(99.99, response.getAmount());
        assertEquals("COMPLETED", response.getStatus());
        assertEquals("2026-01-26T10:15:30Z", response.getTimestamp());
    }

    @Test
    void allArgsConstructor_populatesAllFields() {
        RefundResponse response = new RefundResponse(
                false,
                "Refund failed",
                "txn-1",
                "refund-2",
                10.0,
                "FAILED",
                "2026-01-26T00:00:00Z"
        );

        assertFalse(response.isSuccess());
        assertEquals("Refund failed", response.getMessage());
        assertEquals("txn-1", response.getTransactionId());
        assertEquals("refund-2", response.getRefundId());
        assertEquals(10.0, response.getAmount());
        assertEquals("FAILED", response.getStatus());
        assertEquals("2026-01-26T00:00:00Z", response.getTimestamp());
    }

    @Test
    void gettersReturnDefaultValues_whenUsingNoArgsConstructor() {
        RefundResponse response = new RefundResponse();

        assertFalse(response.isSuccess(), "boolean default should be false");
        assertNull(response.getMessage());
        assertNull(response.getTransactionId());
        assertNull(response.getRefundId());
        assertNull(response.getAmount());
        assertNull(response.getStatus());
        assertNull(response.getTimestamp());
    }

    @Test
    void equalsAndHashCode_coverPositiveAndNegativeCases() {
        RefundResponse r1 = new RefundResponse(
                true,
                "msg",
                "txn-1",
                "refund-1",
                5.0,
                "COMPLETED",
                "ts"
        );

        RefundResponse r2 = new RefundResponse(
                true,
                "msg",
                "txn-1",
                "refund-1",
                5.0,
                "COMPLETED",
                "ts"
        );

        RefundResponse rDifferent = new RefundResponse(
                false,
                "other",
                "txn-2",
                "refund-2",
                10.0,
                "FAILED",
                "other-ts"
        );

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
        RefundResponse response = new RefundResponse(
                true,
                "Refund ok",
                "txn-1",
                "refund-1",
                15.0,
                "COMPLETED",
                "ts"
        );

        String s = response.toString();
        assertNotNull(s);
        assertTrue(s.contains("Refund ok"));
        assertTrue(s.contains("txn-1"));
        assertTrue(s.contains("refund-1"));
        assertTrue(s.contains("COMPLETED"));
    }
}

