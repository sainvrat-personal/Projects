package com.example.orderservice.exception;

/**
 * Domain-specific exception to indicate that a requested resource
 * (e.g., Order) does not exist. Extends IllegalArgumentException so
 * existing catch blocks and tests that expect IllegalArgumentException
 * continue to work without modification.
 */
public class ResourceNotFoundException extends IllegalArgumentException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
