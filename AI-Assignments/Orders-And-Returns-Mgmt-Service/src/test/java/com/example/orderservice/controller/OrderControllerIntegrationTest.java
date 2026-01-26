package com.example.orderservice.controller;

import com.example.orderservice.dto.OrderStatus;
import com.example.orderservice.entity.Order;
import com.example.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@SpringBootTest(classes = com.example.orderservice.OrderReturnsManagementApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:ordercontrollertest",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.profiles.active=test"
})
class OrderControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Test
    void testUpdateOrderStateEndpoint() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID changedBy = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        order.setStatus(OrderStatus.PAID);
        Mockito.when(orderService.transitionOrderState(eq(orderId), eq(OrderStatus.PAID), eq(changedBy))).thenReturn(order);
        mockMvc.perform(patch("/orders/" + orderId + "/state")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newState\": \"PAID\", \"changedBy\": \"" + changedBy + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(OrderStatus.PAID.toString()));
    }

    @Test
    void testCancelOrderEndpoint() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID changedBy = UUID.randomUUID();
        String reason = "Customer request";
        Order order = new Order();
        order.setId(orderId);
        order.setStatus(OrderStatus.CANCELLED);
        Mockito.when(orderService.cancelOrder(eq(orderId), eq(reason), eq(changedBy))).thenReturn(order);
        mockMvc.perform(patch("/orders/" + orderId + "/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"changedBy\": \"" + changedBy + "\", \"reason\": \"" + reason + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(OrderStatus.CANCELLED.toString()));
    }
}
