package com.example.orderservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequest {

    @NotBlank(message = "returnId is required")
    private String returnId;

    @NotBlank(message = "orderId is required")
    private String orderId;

    @NotBlank(message = "transactionId is required")
    private String transactionId;

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be greater than zero")
    private Double amount;

    @NotBlank(message = "customerEmail is required")
    @Email(message = "customerEmail must be a well-formed email address")
    private String customerEmail;

    // Optional descriptive reason; used for simulations and audit
    private String reason;
}