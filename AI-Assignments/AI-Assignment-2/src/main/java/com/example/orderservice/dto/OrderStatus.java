package com.example.orderservice.dto;

/**
 * Order lifecycle:
 * PENDING_PAYMENT → PAID → PROCESSING_IN_WAREHOUSE → SHIPPED → DELIVERED
 * Can be CANCELLED from PENDING_PAYMENT or PAID.
 */
public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    PROCESSING_IN_WAREHOUSE,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
