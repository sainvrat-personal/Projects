package com.example.orderservice.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StateHistoryTest {

    @Test
    void noArgsConstructorAndSetters_populateFieldsCorrectly() {
        StateHistory history = new StateHistory();

        UUID id = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        String entityType = "order";
        UUID entityId = UUID.randomUUID();
        String state = "PENDING";
        UUID changedBy = UUID.randomUUID();
        Instant timestamp = Instant.now().minusSeconds(60);
        Instant createdAt = Instant.now().minusSeconds(120);
        Instant updatedAt = Instant.now().minusSeconds(30);
        String notes = "Some notes";

        history.setId(id);
        history.setTransactionId(transactionId);
        history.setEntityType(entityType);
        history.setEntityId(entityId);
        history.setState(state);
        history.setChangedBy(changedBy);
        history.setTimestamp(timestamp);
        history.setCreatedAt(createdAt);
        history.setUpdatedAt(updatedAt);
        history.setNotes(notes);

        assertEquals(id, history.getId());
        assertEquals(transactionId, history.getTransactionId());
        assertEquals(entityType, history.getEntityType());
        assertEquals(entityId, history.getEntityId());
        assertEquals(state, history.getState());
        assertEquals(changedBy, history.getChangedBy());
        assertEquals(timestamp, history.getTimestamp());
        assertEquals(createdAt, history.getCreatedAt());
        assertEquals(updatedAt, history.getUpdatedAt());
        assertEquals(notes, history.getNotes());
    }

    @Test
    void allArgsConstructor_populatesAllFields() {
        UUID id = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        String entityType = "return";
        UUID entityId = UUID.randomUUID();
        String state = "COMPLETED";
        UUID changedBy = UUID.randomUUID();
        Instant timestamp = Instant.now().minusSeconds(200);
        Instant createdAt = Instant.now().minusSeconds(300);
        Instant updatedAt = Instant.now().minusSeconds(150);
        String notes = "Completion notes";

        StateHistory history = new StateHistory(
                id,
                transactionId,
                entityType,
                entityId,
                state,
                changedBy,
                timestamp,
                createdAt,
                updatedAt,
                notes
        );

        assertEquals(id, history.getId());
        assertEquals(transactionId, history.getTransactionId());
        assertEquals(entityType, history.getEntityType());
        assertEquals(entityId, history.getEntityId());
        assertEquals(state, history.getState());
        assertEquals(changedBy, history.getChangedBy());
        assertEquals(timestamp, history.getTimestamp());
        assertEquals(createdAt, history.getCreatedAt());
        assertEquals(updatedAt, history.getUpdatedAt());
        assertEquals(notes, history.getNotes());
    }

    @Test
    void defaultFieldValues_areInitialized() {
        StateHistory history = new StateHistory();

        assertNull(history.getId());
        assertNull(history.getTransactionId());
        assertNull(history.getEntityType());
        assertNull(history.getEntityId());
        assertNull(history.getState());
        assertNull(history.getChangedBy());
        assertNotNull(history.getTimestamp());
        assertNotNull(history.getCreatedAt());
        assertNotNull(history.getUpdatedAt());
        assertNull(history.getNotes());
    }

    @Test
    void equalsAndHashCode_coverPositiveAndNegativeCases() {
        UUID id = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        UUID entityId = UUID.randomUUID();
        UUID changedBy = UUID.randomUUID();
        Instant timestamp = Instant.now().minusSeconds(50);
        Instant createdAt = Instant.now().minusSeconds(100);
        Instant updatedAt = Instant.now().minusSeconds(25);

        StateHistory h1 = new StateHistory(
                id,
                transactionId,
                "order",
                entityId,
                "PENDING",
                changedBy,
                timestamp,
                createdAt,
                updatedAt,
                "note"
        );

        StateHistory h2 = new StateHistory(
                id,
                transactionId,
                "order",
                entityId,
                "PENDING",
                changedBy,
                timestamp,
                createdAt,
                updatedAt,
                "note"
        );

        StateHistory hDifferent = new StateHistory(
                id,
                transactionId,
                "return",
                entityId,
                "COMPLETED",
                changedBy,
                timestamp,
                createdAt,
                updatedAt,
                "other-note"
        );

        // same instance
        assertEquals(h1, h1);

        // equal values
        assertEquals(h1, h2);
        assertEquals(h1.hashCode(), h2.hashCode());

        // different content
        assertNotEquals(h1, hDifferent);

        // null and different type
        assertNotEquals(h1, null);
        assertNotEquals(h1, new Object());
    }

    @Test
    void toString_containsKeyInformationAndDoesNotThrow() {
        StateHistory history = new StateHistory();
        UUID id = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        history.setId(id);
        history.setTransactionId(transactionId);
        history.setEntityType("order");
        history.setState("PAID");

        String s = history.toString();
        assertNotNull(s);
        assertTrue(s.contains(id.toString()));
        assertTrue(s.contains(transactionId.toString()));
        assertTrue(s.contains("order"));
        assertTrue(s.contains("PAID"));
    }
}

