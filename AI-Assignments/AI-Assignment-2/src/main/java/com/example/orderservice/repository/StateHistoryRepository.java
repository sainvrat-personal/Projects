package com.example.orderservice.repository;

import com.example.orderservice.entity.StateHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StateHistoryRepository extends JpaRepository<StateHistory, UUID> {
    // Custom query methods if needed
    List<StateHistory> findByEntityTypeAndEntityId(String entityType, UUID entityId);

    List<StateHistory> findByEntityTypeAndEntityIdAndState(String entityType, UUID entityId, String state);
}
