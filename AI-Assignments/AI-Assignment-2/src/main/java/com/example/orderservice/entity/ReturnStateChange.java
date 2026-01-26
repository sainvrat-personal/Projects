package com.example.orderservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.Column;

import com.example.orderservice.dto.ReturnStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "return_state_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReturnStateChange {
    @Id
    private UUID id;

    @Column(name = "return_id", nullable = false)
    private UUID returnId;

    @Enumerated(EnumType.STRING)
    private ReturnStatus fromStatus;

    @Enumerated(EnumType.STRING)
    private ReturnStatus toStatus;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "customer_id", nullable = false)
    private UUID changedBy;
}
