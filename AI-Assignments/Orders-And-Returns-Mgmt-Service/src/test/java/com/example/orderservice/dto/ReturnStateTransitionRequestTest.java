package com.example.orderservice.dto;

import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReturnStateTransitionRequestTest {

    @Test
    void noArgsConstructorAndSetters_populateFieldsCorrectly() {
        ReturnStatus newState = ReturnStatus.APPROVED;
        UUID changedBy = UUID.randomUUID();

        ReturnStateTransitionRequest request = new ReturnStateTransitionRequest();
        request.setNewState(newState);
        request.setChangedBy(changedBy);

        assertEquals(newState, request.getNewState());
        assertEquals(changedBy, request.getChangedBy());
    }

    @Test
    void allArgsConstructor_populatesAllFields() {
        ReturnStatus newState = ReturnStatus.IN_TRANSIT;
        UUID changedBy = UUID.randomUUID();

        ReturnStateTransitionRequest request = new ReturnStateTransitionRequest(newState, changedBy);

        assertEquals(newState, request.getNewState());
        assertEquals(changedBy, request.getChangedBy());
    }

    @Test
    void gettersReturnNullByDefault_whenUsingNoArgsConstructor() {
        ReturnStateTransitionRequest request = new ReturnStateTransitionRequest();

        assertNull(request.getNewState());
        assertNull(request.getChangedBy());
    }

    @Test
    void equalsAndHashCode_coverPositiveAndNegativeCases() {
        ReturnStatus newState = ReturnStatus.RECEIVED;
        UUID changedBy = UUID.randomUUID();

        ReturnStateTransitionRequest r1 = new ReturnStateTransitionRequest(newState, changedBy);
        ReturnStateTransitionRequest r2 = new ReturnStateTransitionRequest(newState, changedBy);
        ReturnStateTransitionRequest rDifferent = new ReturnStateTransitionRequest(ReturnStatus.COMPLETED, changedBy);

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
        ReturnStatus newState = ReturnStatus.REQUESTED;
        UUID changedBy = UUID.randomUUID();

        ReturnStateTransitionRequest request = new ReturnStateTransitionRequest(newState, changedBy);

        String s = request.toString();
        assertNotNull(s);
        assertTrue(s.contains("REQUESTED"));
        assertTrue(s.contains(changedBy.toString()));
    }

    @Test
    void validationFails_whenRequiredFieldsAreNull() {
        try {
            Field newState = ReturnStateTransitionRequest.class.getDeclaredField("newState");
            NotNull newStateNotNull = newState.getAnnotation(NotNull.class);
            assertNotNull(newStateNotNull);
            assertEquals("newState is required", newStateNotNull.message());

            Field changedBy = ReturnStateTransitionRequest.class.getDeclaredField("changedBy");
            NotNull changedByNotNull = changedBy.getAnnotation(NotNull.class);
            assertNotNull(changedByNotNull);
            assertEquals("changedBy is required", changedByNotNull.message());
        } catch (NoSuchFieldException e) {
            fail("Expected field not found: " + e.getMessage());
        }
    }
}

