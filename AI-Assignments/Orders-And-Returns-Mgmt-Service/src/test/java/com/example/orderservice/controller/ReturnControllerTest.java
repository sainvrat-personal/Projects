package com.example.orderservice.controller;

import com.example.orderservice.dto.ReturnInitiationRequest;
import com.example.orderservice.dto.ReturnStateTransitionRequest;
import com.example.orderservice.dto.ReturnStatus;
import com.example.orderservice.entity.Return;
import com.example.orderservice.entity.StateHistory;
import com.example.orderservice.service.ReturnService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(ReturnController.class)
class ReturnControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReturnService returnService;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID orderId;
    private UUID returnId;
    private UUID changedBy;
    private Return returnObj;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        returnId = UUID.randomUUID();
        changedBy = UUID.randomUUID();

        returnObj = new Return();
        returnObj.setId(returnId);
        returnObj.setOrderId(orderId);
        returnObj.setStatus(ReturnStatus.REQUESTED);
        returnObj.setCreatedAt(Instant.now());
    }

    // ---------------------------------------------------------------------
    // POST /returns/{orderId}
    // ---------------------------------------------------------------------

    @Test
    void testRequestReturn_Success() throws Exception {
        ReturnInitiationRequest request = new ReturnInitiationRequest();
        request.setChangedBy(changedBy);

        when(returnService.initiateReturn(eq(orderId), eq(changedBy))).thenReturn(returnObj);

        mockMvc.perform(post("/returns/{orderId}", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(returnId.toString()))
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value(ReturnStatus.REQUESTED.name()));
    }

    @Test
    void testRequestReturn_ValidationError_MissingChangedBy() throws Exception {
        ReturnInitiationRequest request = new ReturnInitiationRequest();
        request.setChangedBy(null);

        mockMvc.perform(post("/returns/{orderId}", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void testRequestReturn_OrderNotFound() throws Exception {
        ReturnInitiationRequest request = new ReturnInitiationRequest();
        request.setChangedBy(changedBy);

        when(returnService.initiateReturn(eq(orderId), eq(changedBy)))
                .thenThrow(new IllegalArgumentException("Order not found"));

        mockMvc.perform(post("/returns/{orderId}", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Order not found"));
    }

    @Test
    void testRequestReturn_ServiceException() throws Exception {
        ReturnInitiationRequest request = new ReturnInitiationRequest();
        request.setChangedBy(changedBy);

        when(returnService.initiateReturn(eq(orderId), eq(changedBy)))
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(post("/returns/{orderId}", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }

    // ---------------------------------------------------------------------
    // PATCH /returns/{returnId}/state
    // ---------------------------------------------------------------------

    @Test
    void testUpdateReturnState_Success() throws Exception {
        ReturnStateTransitionRequest request = new ReturnStateTransitionRequest();
        request.setNewState(ReturnStatus.APPROVED);
        request.setChangedBy(changedBy);

        Return updated = new Return();
        updated.setId(returnId);
        updated.setOrderId(orderId);
        updated.setStatus(ReturnStatus.APPROVED);

        when(returnService.transitionReturnState(eq(returnId), eq(ReturnStatus.APPROVED), eq(changedBy)))
                .thenReturn(updated);

        mockMvc.perform(patch("/returns/{returnId}/state", returnId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(returnId.toString()))
                .andExpect(jsonPath("$.status").value(ReturnStatus.APPROVED.name()));
    }

    @Test
    void testUpdateReturnState_InvalidTransition_ThrowsBusinessError() throws Exception {
        ReturnStateTransitionRequest request = new ReturnStateTransitionRequest();
        request.setNewState(ReturnStatus.COMPLETED);
        request.setChangedBy(changedBy);

        // Service returns null to indicate invalid transition
        when(returnService.transitionReturnState(eq(returnId), eq(ReturnStatus.COMPLETED), eq(changedBy)))
                .thenReturn(null);
        when(returnService.getReturnStatus(eq(returnId))).thenReturn(ReturnStatus.REQUESTED);

        mockMvc.perform(patch("/returns/{returnId}/state", returnId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.message").value(
                        "Invalid return state transition from REQUESTED to COMPLETED"));
    }

    @Test
    void testUpdateReturnState_ReturnNotFound() throws Exception {
        ReturnStateTransitionRequest request = new ReturnStateTransitionRequest();
        request.setNewState(ReturnStatus.APPROVED);
        request.setChangedBy(changedBy);

        when(returnService.transitionReturnState(eq(returnId), eq(ReturnStatus.APPROVED), eq(changedBy)))
                .thenThrow(new IllegalArgumentException("Return not found"));

        mockMvc.perform(patch("/returns/{returnId}/state", returnId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Return not found"));
    }

    @Test
    void testUpdateReturnState_ValidationError_MissingNewState() throws Exception {
        ReturnStateTransitionRequest request = new ReturnStateTransitionRequest();
        request.setNewState(null);
        request.setChangedBy(changedBy);

        mockMvc.perform(patch("/returns/{returnId}/state", returnId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void testUpdateReturnState_ValidationError_MissingChangedBy() throws Exception {
        ReturnStateTransitionRequest request = new ReturnStateTransitionRequest();
        request.setNewState(ReturnStatus.APPROVED);
        request.setChangedBy(null);

        mockMvc.perform(patch("/returns/{returnId}/state", returnId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void testUpdateReturnState_ServiceException() throws Exception {
        ReturnStateTransitionRequest request = new ReturnStateTransitionRequest();
        request.setNewState(ReturnStatus.APPROVED);
        request.setChangedBy(changedBy);

        when(returnService.transitionReturnState(eq(returnId), eq(ReturnStatus.APPROVED), eq(changedBy)))
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(patch("/returns/{returnId}/state", returnId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }

    // ---------------------------------------------------------------------
    // GET /returns/{returnId}/state-history
    // ---------------------------------------------------------------------

    @Test
    void testGetReturnStateHistory_Success() throws Exception {
        StateHistory history1 = new StateHistory();
        history1.setEntityType("Return");
        history1.setEntityId(returnId);
        history1.setState(ReturnStatus.REQUESTED.name());
        history1.setTimestamp(Instant.now());

        List<StateHistory> historyList = Arrays.asList(history1);
        when(returnService.getReturnStateHistory(returnId)).thenReturn(historyList);

        mockMvc.perform(get("/returns/{returnId}/state-history", returnId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].entityType").value("Return"))
                .andExpect(jsonPath("$[0].state").value(ReturnStatus.REQUESTED.name()));
    }

    @Test
    void testGetReturnStateHistory_EmptyHistory() throws Exception {
        when(returnService.getReturnStateHistory(returnId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/returns/{returnId}/state-history", returnId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void testGetReturnStateHistory_ReturnNotFound() throws Exception {
        when(returnService.getReturnStateHistory(returnId))
                .thenThrow(new IllegalArgumentException("Return not found: " + returnId));

        mockMvc.perform(get("/returns/{returnId}/state-history", returnId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Return not found: " + returnId));
    }

    @Test
    void testGetReturnStateHistory_ServiceException() throws Exception {
        when(returnService.getReturnStateHistory(returnId))
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/returns/{returnId}/state-history", returnId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }
}

