package com.example.orderservice.repository;

import com.example.orderservice.entity.Return;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReturnRepository extends JpaRepository<Return, UUID> {

    /**
     * Find an existing return record for a given order.
     * <p>
     * We intentionally allow at most one {@code Return} per {@code orderId}
     * at the application layer by treating duplicate initiation attempts as
     * idempotent retries that simply return the already-existing record.
     */
    Return findByOrderId(UUID orderId);
}
