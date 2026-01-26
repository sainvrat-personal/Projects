package com.example.orderservice.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancellationRequest {

    @NotNull(message = "changedBy is required")
    private UUID changedBy;

    @NotBlank(message = "reason is required")
    private String reason;
}