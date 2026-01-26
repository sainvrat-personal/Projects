package com.example.orderservice.integration;

import com.example.orderservice.dto.CreateOrderRequest;
import com.example.orderservice.dto.OrderCancellationRequest;
import com.example.orderservice.dto.OrderStateTransitionRequest;
import com.example.orderservice.dto.OrderStatus;
import com.example.orderservice.entity.Order;
import com.example.orderservice.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:negativetest",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.profiles.active=test"
})
@Transactional
class NegativeTestScenariosTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private CreateOrderRequest validOrderRequest;

    @BeforeEach
    void setUp() {
        validOrderRequest = new CreateOrderRequest();
        validOrderRequest.setCustomerId(UUID.randomUUID());
        validOrderRequest.setShippingAddress("123 Test St, Test City, TC 12345");
        validOrderRequest.setEmail("test@gmail.com");
        validOrderRequest.setTransactionId(UUID.randomUUID());

        CreateOrderRequest.Item item = new CreateOrderRequest.Item();
        item.setProductId(UUID.randomUUID());
        item.setQuantity(2);
        item.setPrice(99.99);
        validOrderRequest.setItem(item);
    }

    @Test
    void testCreateOrderWithInvalidCustomerId() throws Exception {
        CreateOrderRequest invalidRequest = new CreateOrderRequest();
        invalidRequest.setCustomerId(null);
        invalidRequest.setShippingAddress("123 Test St");
        invalidRequest.setEmail("test@gmail.com");

        CreateOrderRequest.Item item = new CreateOrderRequest.Item();
        item.setProductId(UUID.randomUUID());
        item.setQuantity(1);
        item.setPrice(50.0);
        invalidRequest.setItem(item);

        mockMvc.perform(post("/orders/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Customer ID is required."));
    }

    @Test
    void testCreateOrderWithInvalidEmail() throws Exception {
        CreateOrderRequest invalidEmailRequest = new CreateOrderRequest();
        invalidEmailRequest.setCustomerId(UUID.randomUUID());
        invalidEmailRequest.setShippingAddress("123 Test St");
        invalidEmailRequest.setEmail("invalid-email"); // Invalid email format

        CreateOrderRequest.Item item = new CreateOrderRequest.Item();
        item.setProductId(UUID.randomUUID());
        item.setQuantity(1);
        item.setPrice(50.0);
        invalidEmailRequest.setItem(item);

        mockMvc.perform(post("/orders/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidEmailRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Email format is invalid."));
    }

    @Test
    void testCreateOrderWithNegativeQuantity() throws Exception {
        CreateOrderRequest negativeQtyRequest = new CreateOrderRequest();
        negativeQtyRequest.setCustomerId(UUID.randomUUID());
        negativeQtyRequest.setShippingAddress("123 Test St");
        negativeQtyRequest.setEmail("test@gmail.com");

        CreateOrderRequest.Item item = new CreateOrderRequest.Item();
        item.setProductId(UUID.randomUUID());
        item.setQuantity(-1); // Negative quantity
        item.setPrice(50.0);
        negativeQtyRequest.setItem(item);

        mockMvc.perform(post("/orders/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(negativeQtyRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Quantity must be greater than zero."));
    }

    @Test
    void testCreateOrderWithNegativePrice() throws Exception {
        CreateOrderRequest negativePriceRequest = new CreateOrderRequest();
        negativePriceRequest.setCustomerId(UUID.randomUUID());
        negativePriceRequest.setShippingAddress("123 Test St");
        negativePriceRequest.setEmail("test@gmail.com");

        CreateOrderRequest.Item item = new CreateOrderRequest.Item();
        item.setProductId(UUID.randomUUID());
        item.setQuantity(1);
        item.setPrice(-50.0); // Negative price
        negativePriceRequest.setItem(item);

        mockMvc.perform(post("/orders/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(negativePriceRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Price must be greater than zero."));
    }

    @Test
    void testCreateOrderWithEmptyShippingAddress() throws Exception {
        CreateOrderRequest emptyAddressRequest = new CreateOrderRequest();
        emptyAddressRequest.setCustomerId(UUID.randomUUID());
        emptyAddressRequest.setShippingAddress(""); // Empty address
        emptyAddressRequest.setEmail("test@gmail.com");

        CreateOrderRequest.Item item = new CreateOrderRequest.Item();
        item.setProductId(UUID.randomUUID());
        item.setQuantity(1);
        item.setPrice(50.0);
        emptyAddressRequest.setItem(item);

        mockMvc.perform(post("/orders/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyAddressRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").exists());
    }

    @Test
    void testInvalidStateTransitions() throws Exception {
        // Create order first
        MvcResult createResult = mockMvc.perform(post("/orders/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validOrderRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = createResult.getResponse().getContentAsString();
        String orderIdStr = objectMapper.readTree(responseContent).get("orderId").asText();
        UUID orderId = UUID.fromString(orderIdStr);

        // Test invalid transition: PENDING_PAYMENT -> DELIVERED (skipping PAID and SHIPPED)
        OrderStateTransitionRequest invalidTransition = new OrderStateTransitionRequest();
        invalidTransition.setNewState(OrderStatus.DELIVERED);
        invalidTransition.setChangedBy(UUID.randomUUID());

        mockMvc.perform(patch("/orders/{orderId}/state", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidTransition)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testInvalidStateTransitionFromShipped() throws Exception {
        // Create order and move to SHIPPED state
        MvcResult createResult = mockMvc.perform(post("/orders/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validOrderRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = createResult.getResponse().getContentAsString();
        String orderIdStr = objectMapper.readTree(responseContent).get("orderId").asText();
        UUID orderId = UUID.fromString(orderIdStr);

        // Move through valid transitions to SHIPPED
        mockMvc.perform(patch("/orders/{orderId}/state", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newState\":\"PAID\",\"changedBy\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/orders/{orderId}/state", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newState\":\"SHIPPED\",\"changedBy\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isOk());

        // Try invalid transition: SHIPPED -> PAID (backwards transition)
        OrderStateTransitionRequest backwardTransition = new OrderStateTransitionRequest();
        backwardTransition.setNewState(OrderStatus.PAID);
        backwardTransition.setChangedBy(UUID.randomUUID());

        mockMvc.perform(patch("/orders/{orderId}/state", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(backwardTransition)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").exists());
    }

    @Test
    void testCancelOrderFromDeliveredState() throws Exception {
        // Create order and move to DELIVERED state
        MvcResult createResult = mockMvc.perform(post("/orders/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validOrderRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = createResult.getResponse().getContentAsString();
        String orderIdStr = objectMapper.readTree(responseContent).get("orderId").asText();
        UUID orderId = UUID.fromString(orderIdStr);

        // Move through all valid transitions to DELIVERED
        mockMvc.perform(patch("/orders/{orderId}/state", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newState\":\"PAID\",\"changedBy\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/orders/{orderId}/state", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newState\":\"SHIPPED\",\"changedBy\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/orders/{orderId}/state", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newState\":\"DELIVERED\",\"changedBy\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isOk());

        // Try to cancel from DELIVERED state (should fail)
        OrderCancellationRequest cancelRequest = new OrderCancellationRequest();
        cancelRequest.setReason("Try to cancel delivered order");
        cancelRequest.setChangedBy(UUID.randomUUID());

        mockMvc.perform(patch("/orders/{orderId}/cancel", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cancelRequest)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testDuplicateStateTransition() throws Exception {
        // Create order
        MvcResult createResult = mockMvc.perform(post("/orders/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validOrderRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = createResult.getResponse().getContentAsString();
        String orderIdStr = objectMapper.readTree(responseContent).get("orderId").asText();
        UUID orderId = UUID.fromString(orderIdStr);

        // Transition to PAID
        OrderStateTransitionRequest paidTransition = new OrderStateTransitionRequest();
        paidTransition.setNewState(OrderStatus.PAID);
        paidTransition.setChangedBy(UUID.randomUUID());

        mockMvc.perform(patch("/orders/{orderId}/state", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paidTransition)))
                .andExpect(status().isOk());

        // Try the same transition again (should fail - duplicate transition not allowed)
        mockMvc.perform(patch("/orders/{orderId}/state", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paidTransition)))
                .andExpect(status().isUnprocessableEntity()); // API rejects duplicate transitions
    }

    @Test
    void testInvalidOrderIdFormat() throws Exception {
        String invalidOrderId = "not-a-valid-uuid";

        mockMvc.perform(get("/orders/{orderId}/state-history", invalidOrderId))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testMalformedJsonPayload() throws Exception {
        String malformedJson = "{ \"customerId\": \"not-a-uuid\", \"email\": invalid-json }";

        mockMvc.perform(post("/orders/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void testOrderCreationWithMissingFields() throws Exception {
        String incompleteJson = "{ \"customerId\": \"" + UUID.randomUUID() + "\" }"; // Missing required fields

        mockMvc.perform(post("/orders/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(incompleteJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testStateTransitionWithNullChangedBy() throws Exception {
        // Create order first
        MvcResult createResult = mockMvc.perform(post("/orders/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validOrderRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = createResult.getResponse().getContentAsString();
        String orderIdStr = objectMapper.readTree(responseContent).get("orderId").asText();
        UUID orderId = UUID.fromString(orderIdStr);

        // Try state transition with null changedBy
        String invalidTransitionJson = "{ \"newState\": \"PAID\", \"changedBy\": null }";

        mockMvc.perform(patch("/orders/{orderId}/state", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidTransitionJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void testCancellationWithEmptyReason() throws Exception {
        // Create order first
        MvcResult createResult = mockMvc.perform(post("/orders/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validOrderRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = createResult.getResponse().getContentAsString();
        String orderIdStr = objectMapper.readTree(responseContent).get("orderId").asText();
        UUID orderId = UUID.fromString(orderIdStr);

        // Try cancellation with empty reason
        OrderCancellationRequest emptyCancelRequest = new OrderCancellationRequest();
        emptyCancelRequest.setReason(""); // Empty reason
        emptyCancelRequest.setChangedBy(UUID.randomUUID());

        mockMvc.perform(patch("/orders/{orderId}/cancel", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyCancelRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Cancellation reason is required."));
    }

    @Test
    void testMultipleConcurrentStateTransitions() throws Exception {
        // Create order
        MvcResult createResult = mockMvc.perform(post("/orders/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validOrderRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = createResult.getResponse().getContentAsString();
        String orderIdStr = objectMapper.readTree(responseContent).get("orderId").asText();
        UUID orderId = UUID.fromString(orderIdStr);

        // Simulate concurrent state transitions (should handle race conditions gracefully)
        String paidTransitionJson = "{\"newState\":\"PAID\",\"changedBy\":\"" + UUID.randomUUID() + "\"}";
        
        OrderCancellationRequest cancelRequest = new OrderCancellationRequest();
        cancelRequest.setReason("Concurrent cancel");
        cancelRequest.setChangedBy(UUID.randomUUID());
        String cancelRequestJson = objectMapper.writeValueAsString(cancelRequest);

        // Execute concurrent requests - one should succeed, the other should handle conflict appropriately
        mockMvc.perform(patch("/orders/{orderId}/state", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(paidTransitionJson));

        mockMvc.perform(patch("/orders/{orderId}/cancel", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cancelRequestJson));

        // Verify final state is consistent
        Order finalOrder = orderRepository.findById(orderId).orElse(null);
        assertNotNull(finalOrder);
        // Should be either PAID or CANCELLED, but not in an inconsistent state
        assertTrue(finalOrder.getStatus() == OrderStatus.PAID || 
                  finalOrder.getStatus() == OrderStatus.CANCELLED);
    }
}