package com.example.orderservice.controller;

import com.example.orderservice.dto.ReturnInitiationRequest;
import com.example.orderservice.dto.ReturnStateTransitionRequest;
import com.example.orderservice.entity.Return;
import com.example.orderservice.entity.StateHistory;
import com.example.orderservice.service.ReturnService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/returns")
public class ReturnController {
    private final ReturnService returnService;

    @Autowired
    public ReturnController(ReturnService returnService) {
        this.returnService = returnService;
    }

    /**
     * Initiate a return for an order (only allowed for DELIVERED orders)
     * @param orderId The ID of the order to return
     * @param request ReturnInitiationRequest containing who made the change
     * @return Created Return or error response
     */
    @PostMapping("/{orderId}")
    public ResponseEntity<?> requestReturn(
            @PathVariable UUID orderId,
            @Valid @RequestBody ReturnInitiationRequest request) {
        Return returnObj = returnService.initiateReturn(orderId, request.getChangedBy());
        return ResponseEntity.status(201).body(returnObj);
    }

    /**
     * Update the state of a return
     * @param returnId The ID of the return to update
     * @param request ReturnStateTransitionRequest containing the new state and who made the change
     * @return Updated Return or error response
     */
    @PatchMapping("/{returnId}/state")
    public ResponseEntity<?> updateReturnState(
            @PathVariable UUID returnId,
            @Valid @RequestBody ReturnStateTransitionRequest request) {
        Return updatedReturn = returnService.transitionReturnState(returnId, request.getNewState(), request.getChangedBy());
        if (updatedReturn == null) {
            // Service uses null to indicate an invalid state transition
            throw new IllegalStateException(
                    "Invalid return state transition from " + returnService.getReturnStatus(returnId)
                            + " to " + request.getNewState());
        }
        return ResponseEntity.ok(updatedReturn);
    }
    
    /**
     * Get a return state history by ID
     * @param returnId The ID of the return
     * @return List of state history entries or error response
     */
    @GetMapping("/{returnId}/state-history")
    public ResponseEntity<?> getReturnStateHistory(@PathVariable UUID returnId) {
        List<StateHistory> history = returnService.getReturnStateHistory(returnId);
        return ResponseEntity.ok(history);
    }
}
