package com.example.orderservice.integration;

import com.example.orderservice.dto.RefundRequest;
import com.example.orderservice.dto.RefundResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PaymentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testSuccessfulRefundProcessing() throws Exception {
        RefundRequest refundRequest = new RefundRequest();
        refundRequest.setOrderId(UUID.randomUUID().toString());
        refundRequest.setTransactionId(UUID.randomUUID().toString());
        refundRequest.setAmount(150.00);
        refundRequest.setReason("Customer requested full refund");

        MvcResult result = mockMvc.perform(post("/mock-payment/refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refundRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.transactionId").exists())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        RefundResponse response = objectMapper.readValue(responseContent, RefundResponse.class);
        
        assertNotNull(response.getTransactionId());
        assertEquals("COMPLETED", response.getStatus());
        assertEquals(refundRequest.getAmount(), response.getAmount());
    }

    @Test
    void testRefundWithZeroAmount() throws Exception {
        RefundRequest refundRequest = new RefundRequest();
        refundRequest.setOrderId(UUID.randomUUID().toString());
        refundRequest.setAmount(0.0);
        refundRequest.setReason("Zero amount refund");

        mockMvc.perform(post("/mock-payment/refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refundRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void testRefundWithExcessiveAmount() throws Exception {
        RefundRequest refundRequest = new RefundRequest();
        refundRequest.setOrderId(UUID.randomUUID().toString());
        refundRequest.setAmount(10000.0);
        refundRequest.setReason("High amount refund");

        mockMvc.perform(post("/mock-payment/refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refundRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void testRefundWithEmptyReason() throws Exception {
        RefundRequest refundRequest = new RefundRequest();
        refundRequest.setOrderId(UUID.randomUUID().toString());
        refundRequest.setAmount(50.0);
        refundRequest.setReason("");

        mockMvc.perform(post("/mock-payment/refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refundRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void testRefundWithNullOrderId() throws Exception {
        RefundRequest refundRequest = new RefundRequest();
        refundRequest.setOrderId(null);
        refundRequest.setAmount(75.0);
        refundRequest.setReason("Null order ID");

        mockMvc.perform(post("/mock-payment/refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refundRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void testMultipleRefundsForSameOrder() throws Exception {
        String orderId = UUID.randomUUID().toString();

        RefundRequest firstRefund = new RefundRequest();
        firstRefund.setOrderId(orderId);
        firstRefund.setAmount(100.0);
        firstRefund.setReason("First refund");

        mockMvc.perform(post("/mock-payment/refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstRefund)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        RefundRequest secondRefund = new RefundRequest();
        secondRefund.setOrderId(orderId);
        secondRefund.setAmount(50.0);
        secondRefund.setReason("Second refund for same order");

        mockMvc.perform(post("/mock-payment/refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(secondRefund)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void testConcurrentRefundRequests() throws Exception {
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(3);
        final AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < 3; i++) {
            final int requestIndex = i;
            CompletableFuture.runAsync(() -> {
                try {
                    startLatch.await();
                    RefundRequest refundRequest = new RefundRequest();
                    refundRequest.setOrderId(UUID.randomUUID().toString());
                    refundRequest.setAmount(100.0 + requestIndex);
                    refundRequest.setReason("Concurrent refund " + requestIndex);

                    mockMvc.perform(post("/mock-payment/refund")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refundRequest)))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.status").value("COMPLETED"));

                    successCount.incrementAndGet();
                } catch (Exception e) {
                    fail("Concurrent request failed: " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        assertEquals(3, successCount.get());
    }

    @Test
    void testRefundWithSpecialCharactersInReason() throws Exception {
        RefundRequest refundRequest = new RefundRequest();
        refundRequest.setOrderId(UUID.randomUUID().toString());
        refundRequest.setAmount(25.50);
        refundRequest.setReason("Refund with special chars: @#$%^&*()");

        mockMvc.perform(post("/mock-payment/refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refundRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void testRefundWithVeryLongReason() throws Exception {
        StringBuilder sb = new StringBuilder("Very long reason: ");
        for (int i = 0; i < 1000; i++) {
            sb.append("A");
        }
        String longReason = sb.toString();
        
        RefundRequest refundRequest = new RefundRequest();
        refundRequest.setOrderId(UUID.randomUUID().toString());
        refundRequest.setAmount(99.99);
        refundRequest.setReason(longReason);

        mockMvc.perform(post("/mock-payment/refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refundRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }
}