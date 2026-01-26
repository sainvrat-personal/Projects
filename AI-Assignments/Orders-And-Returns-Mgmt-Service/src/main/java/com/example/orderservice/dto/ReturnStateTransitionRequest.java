package com.example.orderservice.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReturnStateTransitionRequest {

    @NotNull(message = "newState is required")
    private ReturnStatus newState;

    @NotNull(message = "changedBy is required")
    private UUID changedBy;
}