package com.example.orderservice.dto;

import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OrderStateTransitionRequestTest {

    @Test
    void noArgsConstructorAndSetters_populateFieldsCorrectly() {
        OrderStatus newState = OrderStatus.PAID;
        UUID changedBy = UUID.randomUUID();

        OrderStateTransitionRequest request = new OrderStateTransitionRequest();
        request.setNewState(newState);
        request.setChangedBy(changedBy);

        assertEquals(newState, request.getNewState());
        assertEquals(changedBy, request.getChangedBy());
    }

    @Test
    void allArgsConstructor_populatesAllFields() {
        OrderStatus newState = OrderStatus.SHIPPED;
        UUID changedBy = UUID.randomUUID();

        OrderStateTransitionRequest request = new OrderStateTransitionRequest(newState, changedBy);

        assertEquals(newState, request.getNewState());
        assertEquals(changedBy, request.getChangedBy());
    }

    @Test
    void gettersReturnNullByDefault_whenUsingNoArgsConstructor() {
        OrderStateTransitionRequest request = new OrderStateTransitionRequest();

        assertNull(request.getNewState());
        assertNull(request.getChangedBy());
    }

    @Test
    void equalsAndHashCode_coverPositiveAndNegativeCases() {
        OrderStatus newState = OrderStatus.DELIVERED;
        UUID changedBy = UUID.randomUUID();

        OrderStateTransitionRequest r1 = new OrderStateTransitionRequest(newState, changedBy);
        OrderStateTransitionRequest r2 = new OrderStateTransitionRequest(newState, changedBy);
        OrderStateTransitionRequest rDifferentState = new OrderStateTransitionRequest(OrderStatus.CANCELLED, changedBy);

        // same instance
        assertEquals(r1, r1);

        // equal values
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());

        // different content
        assertNotEquals(r1, rDifferentState);

        // null and different type
        assertNotEquals(r1, null);
        assertNotEquals(r1, new Object());
    }

    @Test
    void toString_containsKeyInformationAndDoesNotThrow() {
        OrderStatus newState = OrderStatus.PROCESSING_IN_WAREHOUSE;
        UUID changedBy = UUID.randomUUID();

        OrderStateTransitionRequest request = new OrderStateTransitionRequest(newState, changedBy);

        String s = request.toString();
        assertNotNull(s);
        assertTrue(s.contains("PROCESSING_IN_WAREHOUSE"));
        assertTrue(s.contains(changedBy.toString()));
    }

    @Test
    void validationAnnotations_arePresentWithExpectedMessages() {
        try {
            Field newState = OrderStateTransitionRequest.class.getDeclaredField("newState");
            NotNull newStateNotNull = newState.getAnnotation(NotNull.class);
            assertNotNull(newStateNotNull);
            assertEquals("newState is required", newStateNotNull.message());

            Field changedBy = OrderStateTransitionRequest.class.getDeclaredField("changedBy");
            NotNull changedByNotNull = changedBy.getAnnotation(NotNull.class);
            assertNotNull(changedByNotNull);
            assertEquals("changedBy is required", changedByNotNull.message());
        } catch (NoSuchFieldException e) {
            fail("Expected field not found: " + e.getMessage());
        }
    }
}

