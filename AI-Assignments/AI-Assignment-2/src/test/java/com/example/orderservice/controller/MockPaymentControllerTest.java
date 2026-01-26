package com.example.orderservice.controller;

import com.example.orderservice.dto.RefundRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(MockPaymentController.class)
class MockPaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private RefundRequest refundRequest;
    private UUID transactionId;

    @BeforeEach
    void setUp() {
        transactionId = UUID.randomUUID();
        
        refundRequest = new RefundRequest();
        refundRequest.setTransactionId(transactionId.toString());
        refundRequest.setAmount(99.99);
        refundRequest.setReason("Product return");
        refundRequest.setOrderId(UUID.randomUUID().toString());
        refundRequest.setReturnId(UUID.randomUUID().toString());
        refundRequest.setCustomerEmail("test@gmail.com");
    }

    @Test
    void testProcessRefund_Success() throws Exception {
        mockMvc.perform(post("/mock-payment/refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refundRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refundId").exists())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.amount").value(99.99))
                .andExpect(jsonPath("$.transactionId").value(transactionId.toString()))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void testProcessRefund_SimulatedFailure() throws Exception {
        // Given a reason that triggers deterministic failure branch
        refundRequest.setReason("SIMULATE_FAIL_GATEWAY_DOWN");

        mockMvc.perform(post("/mock-payment/refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refundRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.refundId").doesNotExist())
                .andExpect(jsonPath("$.message").value(
                        "Simulated payment gateway failure: SIMULATE_FAIL_GATEWAY_DOWN"));
    }

    @Test
    void testProcessRefund_NullTransactionId() throws Exception {
        refundRequest.setTransactionId(null);

        mockMvc.perform(post("/mock-payment/refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refundRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void testProcessRefund_ZeroAmount() throws Exception {
        refundRequest.setAmount(0.0);

        mockMvc.perform(post("/mock-payment/refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refundRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(0.0));
    }

    @Test
    void testProcessRefund_NegativeAmount() throws Exception {
        refundRequest.setAmount(-50.00);

        mockMvc.perform(post("/mock-payment/refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refundRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(-50.00));
    }

    @Test
    void testProcessRefund_EmptyReason() throws Exception {
        refundRequest.setReason("");

        mockMvc.perform(post("/mock-payment/refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refundRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testProcessRefund_NullReason() throws Exception {
        refundRequest.setReason(null);

        mockMvc.perform(post("/mock-payment/refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refundRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testProcessRefund_MissingRequestBody() throws Exception {
        mockMvc.perform(post("/mock-payment/refund")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testProcessRefund_MalformedJson() throws Exception {
        mockMvc.perform(post("/mock-payment/refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testProcessRefund_LargeAmount() throws Exception {
        refundRequest.setAmount(999999.99);

        mockMvc.perform(post("/mock-payment/refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refundRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(999999.99));
    }

    @Test
    void testProcessRefund_LongReason() throws Exception {
        StringBuilder longReasonBuilder = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longReasonBuilder.append("A");
        }
        String longReason = longReasonBuilder.toString();
        refundRequest.setReason(longReason);

        mockMvc.perform(post("/mock-payment/refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refundRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void testProcessRefund_InterruptedSleepReturns503() throws Exception {
        // Simulate thread being interrupted before controller calls Thread.sleep
        Thread.currentThread().interrupt();

        try {
            mockMvc.perform(post("/mock-payment/refund")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refundRequest)))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value("INTERRUPTED"))
                    .andExpect(jsonPath("$.refundId").doesNotExist())
                    .andExpect(jsonPath("$.amount").value(99.99))
                    .andExpect(jsonPath("$.transactionId").value(transactionId.toString()));
        } finally {
            // Clear interrupted flag so subsequent tests are not affected
            Thread.interrupted();
        }
    }
}