package com.example.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundResponse {
    private boolean success;
    private String message;
    private String transactionId;
    private String refundId;
    private Double amount;
    
    // Additional fields that might be useful
    private String status;
    private String timestamp;
}