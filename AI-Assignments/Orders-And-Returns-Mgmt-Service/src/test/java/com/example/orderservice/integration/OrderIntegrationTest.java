package com.example.orderservice.integration;

import com.example.orderservice.dto.CreateOrderRequest;
import com.example.orderservice.dto.OrderCancellationRequest;
import com.example.orderservice.dto.OrderStatus;
import com.example.orderservice.entity.Order;
import com.example.orderservice.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
public class OrderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    private CreateOrderRequest createOrderRequest;

    @BeforeEach
    void setUp() {
        createOrderRequest = new CreateOrderRequest();
        createOrderRequest.setCustomerId(UUID.randomUUID());
        createOrderRequest.setShippingAddress("123 Test Street, Test City");
        createOrderRequest.setEmail("test@example.com");
        
        CreateOrderRequest.Item item = new CreateOrderRequest.Item();
        item.setProductId(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        item.setQuantity(2);
        item.setPrice(99.99);
        createOrderRequest.setItem(item);
    }

    @Test
    void testCompleteOrderLifecycle() throws Exception {
        // Step 1: Create Order - expect 200 OK, not 201 Created
        MvcResult createResult = mockMvc.perform(post("/orders/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andReturn();

        String responseContent = createResult.getResponse().getContentAsString();
        String orderIdStr = objectMapper.readTree(responseContent).get("orderId").asText();
        UUID orderId = UUID.fromString(orderIdStr);

        // Step 2: Transition to PAID
        mockMvc.perform(patch("/orders/{orderId}/state", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newState\":\"PAID\",\"changedBy\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        // Step 3: Transition to SHIPPED
        mockMvc.perform(patch("/orders/{orderId}/state", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newState\":\"SHIPPED\",\"changedBy\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));

        // Step 4: Transition to DELIVERED
        mockMvc.perform(patch("/orders/{orderId}/state", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newState\":\"DELIVERED\",\"changedBy\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));

        // Verify final state in database
        Order finalOrder = orderRepository.findById(orderId).orElse(null);
        assertNotNull(finalOrder);
        assertEquals(OrderStatus.DELIVERED, finalOrder.getStatus());
    }

    @Test
    void testOrderCreation() throws Exception {
        mockMvc.perform(post("/orders/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void testOrderCancellationFlow() throws Exception {
        // Step 1: Create Order - expect 200 OK
        MvcResult createResult = mockMvc.perform(post("/orders/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = createResult.getResponse().getContentAsString();
        String orderIdStr = objectMapper.readTree(responseContent).get("orderId").asText();
        UUID orderId = UUID.fromString(orderIdStr);

        // Step 2: Cancel order from PENDING_PAYMENT state
        OrderCancellationRequest cancelRequest = new OrderCancellationRequest();
        cancelRequest.setReason("Customer requested cancellation");
        cancelRequest.setChangedBy(UUID.randomUUID());

        mockMvc.perform(patch("/orders/{orderId}/cancel", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cancelRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Verify cancellation in database
        Order cancelledOrder = orderRepository.findById(orderId).orElse(null);
        assertNotNull(cancelledOrder);
        assertEquals(OrderStatus.CANCELLED, cancelledOrder.getStatus());
    }

    @Test
    void testOrderCancellationFromPaidState() throws Exception {
        // Step 1: Create and pay for order - expect 200 OK
        MvcResult createResult = mockMvc.perform(post("/orders/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = createResult.getResponse().getContentAsString();
        String orderIdStr = objectMapper.readTree(responseContent).get("orderId").asText();
        UUID orderId = UUID.fromString(orderIdStr);

        // Step 2: Transition to PAID
        mockMvc.perform(patch("/orders/{orderId}/state", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newState\":\"PAID\",\"changedBy\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isOk());

        // Step 3: Try to cancel from PAID state (should require refund)
        OrderCancellationRequest cancelRequest = new OrderCancellationRequest();
        cancelRequest.setReason("Customer requested cancellation");
        cancelRequest.setChangedBy(UUID.randomUUID());

        mockMvc.perform(patch("/orders/{orderId}/cancel", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cancelRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void testMultipleOrderCreation() throws Exception {
        // Create first order - expect 200 OK
        mockMvc.perform(post("/orders/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").exists());

        // Create second order - expect 200 OK
        CreateOrderRequest secondRequest = new CreateOrderRequest();
        secondRequest.setCustomerId(UUID.randomUUID());
        secondRequest.setShippingAddress("456 Another Street, Test City");
        secondRequest.setEmail("test2@example.com");
        
        CreateOrderRequest.Item secondItem = new CreateOrderRequest.Item();
        secondItem.setProductId(UUID.fromString("550e8400-e29b-41d4-a716-446655440001"));
        secondItem.setQuantity(1);
        secondItem.setPrice(49.99);
        secondRequest.setItem(secondItem);

        mockMvc.perform(post("/orders/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").exists());
    }
}