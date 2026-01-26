package com.example.orderservice.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReturnStatusEnumTest {

    @Test
    void values_containsAllExpectedStatusesInOrder() {
        ReturnStatus[] expected = {
                ReturnStatus.REQUESTED,
                ReturnStatus.APPROVED,
                ReturnStatus.REJECTED,
                ReturnStatus.IN_TRANSIT,
                ReturnStatus.RECEIVED,
                ReturnStatus.COMPLETED
        };

        ReturnStatus[] actual = ReturnStatus.values();

        assertArrayEquals(expected, actual);
    }

    @Test
    void valueOf_returnsCorrectEnumForEachName() {
        assertEquals(ReturnStatus.REQUESTED, ReturnStatus.valueOf("REQUESTED"));
        assertEquals(ReturnStatus.APPROVED, ReturnStatus.valueOf("APPROVED"));
        assertEquals(ReturnStatus.REJECTED, ReturnStatus.valueOf("REJECTED"));
        assertEquals(ReturnStatus.IN_TRANSIT, ReturnStatus.valueOf("IN_TRANSIT"));
        assertEquals(ReturnStatus.RECEIVED, ReturnStatus.valueOf("RECEIVED"));
        assertEquals(ReturnStatus.COMPLETED, ReturnStatus.valueOf("COMPLETED"));
    }

    @Test
    void valueOf_throwsExceptionForInvalidName() {
        assertThrows(IllegalArgumentException.class, () -> ReturnStatus.valueOf("UNKNOWN_STATUS"));
    }

    @Test
    void toString_returnsNameOfEnumConstant() {
        for (ReturnStatus status : ReturnStatus.values()) {
            assertEquals(status.name(), status.toString());
        }
    }

    @Test
    void ordinal_isNonNegativeAndUnique() {
        boolean[] seen = new boolean[ReturnStatus.values().length];

        for (ReturnStatus status : ReturnStatus.values()) {
            int ordinal = status.ordinal();
            assertTrue(ordinal >= 0);
            assertFalse(seen[ordinal], "Duplicate ordinal " + ordinal + " for " + status);
            seen[ordinal] = true;
        }
    }

    @Test
    void canTransitionTo_allowsOnlyValidForwardTransitions() {
        // REQUESTED -> APPROVED or REJECTED
        assertTrue(ReturnStatus.REQUESTED.canTransitionTo(ReturnStatus.APPROVED));
        assertTrue(ReturnStatus.REQUESTED.canTransitionTo(ReturnStatus.REJECTED));
        assertFalse(ReturnStatus.REQUESTED.canTransitionTo(ReturnStatus.IN_TRANSIT));
        assertFalse(ReturnStatus.REQUESTED.canTransitionTo(ReturnStatus.RECEIVED));
        assertFalse(ReturnStatus.REQUESTED.canTransitionTo(ReturnStatus.COMPLETED));
        assertFalse(ReturnStatus.REQUESTED.canTransitionTo(ReturnStatus.REQUESTED));

        // APPROVED -> IN_TRANSIT only
        assertTrue(ReturnStatus.APPROVED.canTransitionTo(ReturnStatus.IN_TRANSIT));
        assertFalse(ReturnStatus.APPROVED.canTransitionTo(ReturnStatus.REQUESTED));
        assertFalse(ReturnStatus.APPROVED.canTransitionTo(ReturnStatus.REJECTED));
        assertFalse(ReturnStatus.APPROVED.canTransitionTo(ReturnStatus.RECEIVED));
        assertFalse(ReturnStatus.APPROVED.canTransitionTo(ReturnStatus.COMPLETED));
        assertFalse(ReturnStatus.APPROVED.canTransitionTo(ReturnStatus.APPROVED));

        // IN_TRANSIT -> RECEIVED only
        assertTrue(ReturnStatus.IN_TRANSIT.canTransitionTo(ReturnStatus.RECEIVED));
        assertFalse(ReturnStatus.IN_TRANSIT.canTransitionTo(ReturnStatus.REQUESTED));
        assertFalse(ReturnStatus.IN_TRANSIT.canTransitionTo(ReturnStatus.APPROVED));
        assertFalse(ReturnStatus.IN_TRANSIT.canTransitionTo(ReturnStatus.REJECTED));
        assertFalse(ReturnStatus.IN_TRANSIT.canTransitionTo(ReturnStatus.COMPLETED));
        assertFalse(ReturnStatus.IN_TRANSIT.canTransitionTo(ReturnStatus.IN_TRANSIT));

        // RECEIVED -> COMPLETED only
        assertTrue(ReturnStatus.RECEIVED.canTransitionTo(ReturnStatus.COMPLETED));
        assertFalse(ReturnStatus.RECEIVED.canTransitionTo(ReturnStatus.REQUESTED));
        assertFalse(ReturnStatus.RECEIVED.canTransitionTo(ReturnStatus.APPROVED));
        assertFalse(ReturnStatus.RECEIVED.canTransitionTo(ReturnStatus.REJECTED));
        assertFalse(ReturnStatus.RECEIVED.canTransitionTo(ReturnStatus.IN_TRANSIT));
        assertFalse(ReturnStatus.RECEIVED.canTransitionTo(ReturnStatus.RECEIVED));
    }

    @Test
    void canTransitionTo_disallowsTransitionsFromTerminalStatesAndNullTargets() {
        // REJECTED: no forward transitions
        assertFalse(ReturnStatus.REJECTED.canTransitionTo(ReturnStatus.REQUESTED));
        assertFalse(ReturnStatus.REJECTED.canTransitionTo(ReturnStatus.APPROVED));
        assertFalse(ReturnStatus.REJECTED.canTransitionTo(ReturnStatus.IN_TRANSIT));
        assertFalse(ReturnStatus.REJECTED.canTransitionTo(ReturnStatus.RECEIVED));
        assertFalse(ReturnStatus.REJECTED.canTransitionTo(ReturnStatus.COMPLETED));
        assertFalse(ReturnStatus.REJECTED.canTransitionTo(ReturnStatus.REJECTED));

        // COMPLETED: no forward transitions
        assertFalse(ReturnStatus.COMPLETED.canTransitionTo(ReturnStatus.REQUESTED));
        assertFalse(ReturnStatus.COMPLETED.canTransitionTo(ReturnStatus.APPROVED));
        assertFalse(ReturnStatus.COMPLETED.canTransitionTo(ReturnStatus.REJECTED));
        assertFalse(ReturnStatus.COMPLETED.canTransitionTo(ReturnStatus.IN_TRANSIT));
        assertFalse(ReturnStatus.COMPLETED.canTransitionTo(ReturnStatus.RECEIVED));
        assertFalse(ReturnStatus.COMPLETED.canTransitionTo(ReturnStatus.COMPLETED));

        // Edge case: null target should never be allowed
        for (ReturnStatus status : ReturnStatus.values()) {
            assertFalse(status.canTransitionTo(null));
        }
    }
}

