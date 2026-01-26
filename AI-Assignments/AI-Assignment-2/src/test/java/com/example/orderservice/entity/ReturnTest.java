package com.example.orderservice.entity;

import com.example.orderservice.dto.ReturnStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReturnTest {

    @Test
    void noArgsConstructorAndSetters_populateFieldsCorrectly() {
        Return ret = new Return();

        UUID id = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        ReturnStatus status = ReturnStatus.REQUESTED;
        Instant createdAt = Instant.now().minusSeconds(60);
        Long version = 1L;

        ret.setId(id);
        ret.setOrderId(orderId);
        ret.setStatus(status);
        ret.setCreatedAt(createdAt);
        ret.setVersion(version);

        assertEquals(id, ret.getId());
        assertEquals(orderId, ret.getOrderId());
        assertEquals(status, ret.getStatus());
        assertEquals(createdAt, ret.getCreatedAt());
        assertEquals(version, ret.getVersion());
    }

    @Test
    void allArgsConstructor_populatesAllFields() {
        UUID id = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        ReturnStatus status = ReturnStatus.APPROVED;
        Instant createdAt = Instant.now().minusSeconds(120);
        Long version = 2L;

        Return ret = new Return(id, orderId, status, createdAt, version);

        assertEquals(id, ret.getId());
        assertEquals(orderId, ret.getOrderId());
        assertEquals(status, ret.getStatus());
        assertEquals(createdAt, ret.getCreatedAt());
        assertEquals(version, ret.getVersion());
    }

    @Test
    void defaultFieldValues_areInitialized() {
        Return ret = new Return();

        assertNull(ret.getId());
        assertNull(ret.getOrderId());
        assertNull(ret.getStatus());
        assertNotNull(ret.getCreatedAt());
        assertNull(ret.getVersion());
    }

    @Test
    void equalsAndHashCode_coverPositiveAndNegativeCases() {
        UUID id = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Instant createdAt = Instant.now().minusSeconds(30);

        Return r1 = new Return(id, orderId, ReturnStatus.REQUESTED, createdAt, 1L);
        Return r2 = new Return(id, orderId, ReturnStatus.REQUESTED, createdAt, 1L);
        Return rDifferent = new Return(id, orderId, ReturnStatus.COMPLETED, createdAt, 2L);

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
        Return ret = new Return();
        UUID id = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        ret.setId(id);
        ret.setOrderId(orderId);
        ret.setStatus(ReturnStatus.RECEIVED);

        String s = ret.toString();
        assertNotNull(s);
        assertTrue(s.contains(id.toString()));
        assertTrue(s.contains(orderId.toString()));
        assertTrue(s.contains(ReturnStatus.RECEIVED.name()));
    }
}

