package com.example.orderservice.controller;

import com.example.orderservice.dto.RefundRequest;
import com.example.orderservice.dto.RefundResponse;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Random;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mock-payment")
public class MockPaymentController {

    private final Random random = new Random();

    @PostMapping("/refund")
    public ResponseEntity<RefundResponse> processRefund(@Valid @RequestBody RefundRequest request) {
        // Simulate refund processing
        String returnId = request.getReturnId();
        Double amount = request.getAmount();
        String reason = request.getReason();
        
        // Generate a mock refund ID
        String refundId = "REF-" + UUID.randomUUID().toString().substring(0, 8);
        
        // Simulate processing time (50-150ms)
        try {
            Thread.sleep(random.nextInt(100) + 50);
        } catch (InterruptedException e) {
            // In a real gateway, an interrupt would typically abort processing
            // rather than silently continuing. Reflect that behaviour here by
            // restoring the interrupt flag and returning a clear failure
            // response instead of proceeding as if the refund had completed.
            Thread.currentThread().interrupt();

            RefundResponse interrupted = new RefundResponse();
            interrupted.setTimestamp(Instant.now().toString());
            interrupted.setTransactionId(request.getTransactionId());
            interrupted.setAmount(amount);
            interrupted.setRefundId(null);
            interrupted.setSuccess(false);
            interrupted.setStatus("INTERRUPTED");
            interrupted.setMessage("Refund processing was interrupted before completion");
            return ResponseEntity.status(503).body(interrupted);
        }
        
        // Deterministic failure simulation hook for tests/clients:
        // if the reason starts with "SIMULATE_FAIL", treat this as an explicit
        // gateway-level business failure instead of always succeeding. This
        // avoids randomness while still allowing realistic failure scenarios
        // to be exercised end-to-end.
        if (reason != null && reason.startsWith("SIMULATE_FAIL")) {
            RefundResponse response = new RefundResponse();
            response.setTimestamp(Instant.now().toString());
            response.setTransactionId(request.getTransactionId());
            response.setAmount(amount);
            response.setRefundId(null);
            response.setSuccess(false);
            response.setStatus("FAILED");
            response.setMessage("Simulated payment gateway failure: " + reason);
            // Keep HTTP 200 so that clients must inspect the body, just like
            // with real-world gateways that encode business failures in the
            // payload rather than the status code.
            return ResponseEntity.ok(response);
        }

        // Default behaviour: succeed for predictable testing when no explicit
        // failure trigger is provided.
        boolean success = true;
        
        RefundResponse response = new RefundResponse();
        response.setTimestamp(Instant.now().toString());
        response.setTransactionId(request.getTransactionId());
        response.setAmount(amount);
        response.setRefundId(refundId);
        
        if (success) {
            response.setSuccess(true);
            response.setStatus("COMPLETED");
            response.setMessage("Refund of $" + amount + " for return " + returnId + " processed successfully");
            return ResponseEntity.ok(response);
        } else {
            // Simulate occasional payment gateway errors
            response.setSuccess(false);
            response.setStatus("FAILED");
            response.setMessage("Payment gateway error: Transaction declined by bank");
            return ResponseEntity.badRequest().body(response);
        }
    }
}
