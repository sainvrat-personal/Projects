package com.example.orderservice.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStateTransitionRequest {

    @NotNull(message = "newState is required")
    private OrderStatus newState;

    @NotNull(message = "changedBy is required")
    private UUID changedBy;
}