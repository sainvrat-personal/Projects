package com.example.orderservice.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "State_History")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StateHistory {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "entity_type", nullable = false)
    private String entityType; // 'order' or 'return'

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "state", nullable = false)
    private String state;

    @Column(name = "changed_by")
    private UUID changedBy;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp = Instant.now();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
    
    @Column(name = "notes", length = 1000)
    private String notes; // For storing additional information like response messages
}
