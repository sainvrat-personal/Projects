package com.example.orderservice.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResourceNotFoundExceptionTest {

    @Test
    void constructor_setsMessageAndExtendsIllegalArgumentException() {
        String message = "Order not found";

        ResourceNotFoundException ex = new ResourceNotFoundException(message);

        assertEquals(message, ex.getMessage());
        assertTrue(ex instanceof IllegalArgumentException);
    }

    @Test
    void constructor_allowsNullMessage() {
        ResourceNotFoundException ex = new ResourceNotFoundException(null);

        assertNull(ex.getMessage());
    }

    @Test
    void constructor_allowsEmptyMessage() {
        ResourceNotFoundException ex = new ResourceNotFoundException("");

        assertEquals("", ex.getMessage());
    }
}

