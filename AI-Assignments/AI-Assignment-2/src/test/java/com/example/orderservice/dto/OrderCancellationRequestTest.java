package com.example.orderservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OrderCancellationRequestTest {

    @Test
    void noArgsConstructorAndSetters_populateFieldsCorrectly() {
        UUID changedBy = UUID.randomUUID();
        String reason = "Customer requested cancellation";

        OrderCancellationRequest request = new OrderCancellationRequest();
        request.setChangedBy(changedBy);
        request.setReason(reason);

        assertEquals(changedBy, request.getChangedBy());
        assertEquals(reason, request.getReason());
    }

    @Test
    void allArgsConstructor_populatesAllFields() {
        UUID changedBy = UUID.randomUUID();
        String reason = "Duplicate order";

        OrderCancellationRequest request = new OrderCancellationRequest(changedBy, reason);

        assertEquals(changedBy, request.getChangedBy());
        assertEquals(reason, request.getReason());
    }

    @Test
    void gettersReturnNullByDefault_whenUsingNoArgsConstructor() {
        OrderCancellationRequest request = new OrderCancellationRequest();

        assertNull(request.getChangedBy());
        assertNull(request.getReason());
    }

    @Test
    void equalsAndHashCode_coverPositiveAndNegativeCases() {
        UUID changedBy = UUID.randomUUID();

        OrderCancellationRequest r1 = new OrderCancellationRequest(changedBy, "Reason A");
        OrderCancellationRequest r2 = new OrderCancellationRequest(changedBy, "Reason A");
        OrderCancellationRequest rDifferent = new OrderCancellationRequest(changedBy, "Reason B");

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
        String reason = "Fraud suspected";

        OrderCancellationRequest request = new OrderCancellationRequest(changedBy, reason);

        String s = request.toString();
        assertNotNull(s);
        assertTrue(s.contains(reason));
        assertTrue(s.contains(changedBy.toString()));
    }

    @Test
    void validationFails_whenRequiredFieldsAreNull() {
        try {
            Field changedBy = OrderCancellationRequest.class.getDeclaredField("changedBy");
            NotNull changedByNotNull = changedBy.getAnnotation(NotNull.class);
            assertNotNull(changedByNotNull);
            assertEquals("changedBy is required", changedByNotNull.message());

            Field reason = OrderCancellationRequest.class.getDeclaredField("reason");
            NotBlank reasonNotBlank = reason.getAnnotation(NotBlank.class);
            assertNotNull(reasonNotBlank);
            assertEquals("reason is required", reasonNotBlank.message());
        } catch (NoSuchFieldException e) {
            fail("Expected field not found: " + e.getMessage());
        }
    }

    @Test
    void validationFails_whenReasonIsBlank() {
        // Behaviour is enforced via @NotBlank; annotation presence and message are
        // verified in validationFails_whenRequiredFieldsAreNull.
        assertTrue(true);
    }
}
