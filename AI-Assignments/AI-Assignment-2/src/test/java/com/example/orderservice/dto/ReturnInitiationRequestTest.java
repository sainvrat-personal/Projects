package com.example.orderservice.dto;

import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReturnInitiationRequestTest {

    @Test
    void noArgsConstructorAndSetters_populateFieldsCorrectly() {
        UUID changedBy = UUID.randomUUID();

        ReturnInitiationRequest request = new ReturnInitiationRequest();
        request.setChangedBy(changedBy);

        assertEquals(changedBy, request.getChangedBy());
    }

    @Test
    void allArgsConstructor_populatesAllFields() {
        UUID changedBy = UUID.randomUUID();

        ReturnInitiationRequest request = new ReturnInitiationRequest(changedBy);

        assertEquals(changedBy, request.getChangedBy());
    }

    @Test
    void gettersReturnNullByDefault_whenUsingNoArgsConstructor() {
        ReturnInitiationRequest request = new ReturnInitiationRequest();

        assertNull(request.getChangedBy());
    }

    @Test
    void equalsAndHashCode_coverPositiveAndNegativeCases() {
        UUID changedBy = UUID.randomUUID();

        ReturnInitiationRequest r1 = new ReturnInitiationRequest(changedBy);
        ReturnInitiationRequest r2 = new ReturnInitiationRequest(changedBy);
        ReturnInitiationRequest rDifferent = new ReturnInitiationRequest(UUID.randomUUID());

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
        UUID changedBy = UUID.randomUUID();
        ReturnInitiationRequest request = new ReturnInitiationRequest(changedBy);

        String s = request.toString();
        assertNotNull(s);
        assertTrue(s.contains(changedBy.toString()));
    }

    @Test
    void validationFails_whenChangedByIsNull() {
        try {
            Field changedBy = ReturnInitiationRequest.class.getDeclaredField("changedBy");
            NotNull changedByNotNull = changedBy.getAnnotation(NotNull.class);
            assertNotNull(changedByNotNull);
            assertEquals("changedBy is required", changedByNotNull.message());
        } catch (NoSuchFieldException e) {
            fail("Expected field not found: " + e.getMessage());
        }
    }
}

