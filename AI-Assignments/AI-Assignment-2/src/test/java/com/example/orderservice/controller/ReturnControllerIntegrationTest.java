package com.example.orderservice.controller;

import com.example.orderservice.dto.ReturnInitiationRequest;
import com.example.orderservice.dto.ReturnStatus;
import com.example.orderservice.entity.Return;
import com.example.orderservice.service.ReturnService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.mockito.ArgumentMatchers.eq;

@SpringBootTest(classes = com.example.orderservice.OrderReturnsManagementApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:returncontrollertest",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.profiles.active=test"
})
class ReturnControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReturnService returnService;

    @Test
    void testRequestReturnEndpoint() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID changedBy = UUID.randomUUID();
        
        Return returnObj = new Return();
        returnObj.setId(UUID.randomUUID());
        returnObj.setOrderId(orderId);
        returnObj.setStatus(ReturnStatus.REQUESTED);
        
        Mockito.when(returnService.initiateReturn(eq(orderId), eq(changedBy))).thenReturn(returnObj);
        
        mockMvc.perform(post("/returns/" + orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"changedBy\": \"" + changedBy + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(ReturnStatus.REQUESTED.toString()));
    }

    @Test
    void testUpdateReturnStateEndpoint() throws Exception {
        UUID returnId = UUID.randomUUID();
        UUID changedBy = UUID.randomUUID();
        
        Return returnObj = new Return();
        returnObj.setOrderId(UUID.randomUUID());
        returnObj.setId(returnId);
        returnObj.setStatus(ReturnStatus.APPROVED);
        
        Mockito.when(returnService.transitionReturnState(eq(returnId), eq(ReturnStatus.APPROVED), eq(changedBy)))
               .thenReturn(returnObj);
        
        mockMvc.perform(patch("/returns/" + returnId + "/state")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newState\": \"APPROVED\", \"changedBy\": \"" + changedBy + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(ReturnStatus.APPROVED.toString()));
    }
}
