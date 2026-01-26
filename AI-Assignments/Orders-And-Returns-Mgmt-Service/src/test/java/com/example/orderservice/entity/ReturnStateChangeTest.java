package com.example.orderservice.entity;

import com.example.orderservice.dto.ReturnStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReturnStateChangeTest {

    @Test
    void noArgsConstructorAndSetters_populateFieldsCorrectly() {
        ReturnStateChange change = new ReturnStateChange();

        UUID id = UUID.randomUUID();
        UUID returnId = UUID.randomUUID();
        ReturnStatus fromStatus = ReturnStatus.REQUESTED;
        ReturnStatus toStatus = ReturnStatus.APPROVED;
        Instant updatedAt = Instant.now().minusSeconds(60);
        UUID changedBy = UUID.randomUUID();

        change.setId(id);
        change.setReturnId(returnId);
        change.setFromStatus(fromStatus);
        change.setToStatus(toStatus);
        change.setUpdatedAt(updatedAt);
        change.setChangedBy(changedBy);

        assertEquals(id, change.getId());
        assertEquals(returnId, change.getReturnId());
        assertEquals(fromStatus, change.getFromStatus());
        assertEquals(toStatus, change.getToStatus());
        assertEquals(updatedAt, change.getUpdatedAt());
        assertEquals(changedBy, change.getChangedBy());
    }

    @Test
    void allArgsConstructor_populatesAllFields() {
        UUID id = UUID.randomUUID();
        UUID returnId = UUID.randomUUID();
        ReturnStatus fromStatus = ReturnStatus.APPROVED;
        ReturnStatus toStatus = ReturnStatus.IN_TRANSIT;
        Instant updatedAt = Instant.now().minusSeconds(120);
        UUID changedBy = UUID.randomUUID();

        ReturnStateChange change = new ReturnStateChange(
                id,
                returnId,
                fromStatus,
                toStatus,
                updatedAt,
                changedBy
        );

        assertEquals(id, change.getId());
        assertEquals(returnId, change.getReturnId());
        assertEquals(fromStatus, change.getFromStatus());
        assertEquals(toStatus, change.getToStatus());
        assertEquals(updatedAt, change.getUpdatedAt());
        assertEquals(changedBy, change.getChangedBy());
    }

    @Test
    void defaultFieldValues_areInitialized() {
        ReturnStateChange change = new ReturnStateChange();

        assertNull(change.getId());
        assertNull(change.getReturnId());
        assertNull(change.getFromStatus());
        assertNull(change.getToStatus());
        assertNotNull(change.getUpdatedAt());
        assertNull(change.getChangedBy());
    }

    @Test
    void equalsAndHashCode_coverPositiveAndNegativeCases() {
        UUID id = UUID.randomUUID();
        UUID returnId = UUID.randomUUID();
        Instant updatedAt = Instant.now().minusSeconds(30);
        UUID changedBy = UUID.randomUUID();

        ReturnStateChange c1 = new ReturnStateChange(
                id,
                returnId,
                ReturnStatus.REQUESTED,
                ReturnStatus.APPROVED,
                updatedAt,
                changedBy
        );

        ReturnStateChange c2 = new ReturnStateChange(
                id,
                returnId,
                ReturnStatus.REQUESTED,
                ReturnStatus.APPROVED,
                updatedAt,
                changedBy
        );

        ReturnStateChange cDifferent = new ReturnStateChange(
                id,
                returnId,
                ReturnStatus.REJECTED,
                ReturnStatus.COMPLETED,
                updatedAt,
                changedBy
        );

        // same instance
        assertEquals(c1, c1);

        // equal values
        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());

        // different content
        assertNotEquals(c1, cDifferent);

        // null and different type
        assertNotEquals(c1, null);
        assertNotEquals(c1, new Object());
    }

    @Test
    void toString_containsKeyInformationAndDoesNotThrow() {
        ReturnStateChange change = new ReturnStateChange();
        UUID id = UUID.randomUUID();
        UUID returnId = UUID.randomUUID();
        change.setId(id);
        change.setReturnId(returnId);
        change.setFromStatus(ReturnStatus.REQUESTED);
        change.setToStatus(ReturnStatus.APPROVED);

        String s = change.toString();
        assertNotNull(s);
        assertTrue(s.contains(id.toString()));
        assertTrue(s.contains(returnId.toString()));
        assertTrue(s.contains(ReturnStatus.REQUESTED.name()));
        assertTrue(s.contains(ReturnStatus.APPROVED.name()));
    }
}

