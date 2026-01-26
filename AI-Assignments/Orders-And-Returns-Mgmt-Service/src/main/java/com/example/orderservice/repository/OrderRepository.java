package com.example.orderservice.repository;

import com.example.orderservice.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    /**
     * Find an order by its transactionId. This is used to provide
     * idempotency for order creation when clients reuse transaction IDs.
     */
    Optional<Order> findByTransactionId(UUID transactionId);
}
