package com.example.orderservice.service;

import com.example.orderservice.dto.OrderStatus;
import com.example.orderservice.dto.ReturnStatus;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.Return;
import com.example.orderservice.entity.StateHistory;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.repository.ReturnRepository;
import com.example.orderservice.repository.ReturnStateChangeRepository;
import com.example.orderservice.repository.StateHistoryRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReturnServiceTest {
    @Mock
    private ReturnStateChangeRepository returnStateChangeRepository;
    
    @Mock
    private JobScheduler jobScheduler;
    
    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private ReturnRepository returnRepository;
    
    @Mock
    private StateHistoryRepository stateHistoryRepository;
    
    @InjectMocks
    private ReturnService returnService;
    
    private UUID orderId;
    private UUID returnId;
    private UUID changedBy;
    private Order order;
    private Return returnObj;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        orderId = UUID.randomUUID();
        returnId = UUID.randomUUID();
        changedBy = UUID.randomUUID();
        
        order = new Order();
        order.setId(orderId);
        order.setTransactionId(UUID.randomUUID()); // Add transactionId
        order.setStatus(OrderStatus.DELIVERED);
        
        returnObj = new Return();
        returnObj.setOrderId(orderId);
        returnObj.setId(returnId);
        returnObj.setStatus(ReturnStatus.REQUESTED);
    }
    
    @Test
    void testInitiateReturnForDeliveredOrder_createsReturnStateChangeAndAudit() {
        when(returnRepository.findByOrderId(orderId)).thenReturn(null);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(returnRepository.save(any(Return.class))).thenAnswer(i -> i.getArgument(0));
        when(returnStateChangeRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        
        Return result = returnService.initiateReturn(orderId, changedBy);
        
        assertNotNull(result);
        assertEquals(ReturnStatus.REQUESTED, result.getStatus());

        // Verify that a state change and audit history were recorded
        verify(returnStateChangeRepository, times(1)).save(any());
        ArgumentCaptor<StateHistory> historyCaptor = ArgumentCaptor.forClass(StateHistory.class);
        verify(stateHistoryRepository, times(1)).save(historyCaptor.capture());
        StateHistory history = historyCaptor.getValue();
        assertEquals("Return", history.getEntityType());
        assertEquals(result.getId(), history.getEntityId());
        assertEquals(ReturnStatus.REQUESTED.name(), history.getState());
    }

    @Test
    void testInitiateReturn_IdempotentWhenReturnAlreadyExists_doesNotCreateNewArtifacts() {
        // Given an existing return for this order
        when(returnRepository.findByOrderId(orderId)).thenReturn(returnObj);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When a second initiation request comes in (e.g., from another device)
        Return result = returnService.initiateReturn(orderId, changedBy);

        // Then the existing return is reused and no duplicate is created
        assertNotNull(result);
        assertEquals(returnObj.getId(), result.getId());
        assertEquals(ReturnStatus.REQUESTED, result.getStatus());

        verify(returnRepository, never()).save(any(Return.class));
        verify(returnStateChangeRepository, never()).save(any());
        verify(stateHistoryRepository, never()).save(any());
    }
    
    @Test
    void testInitiateReturnNotAllowedForNonDeliveredOrder_recordsAuditAndThrows() {
        order.setStatus(OrderStatus.PAID);
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        when(returnRepository.findByOrderId(orderId)).thenReturn(null);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        
        Exception ex = assertThrows(IllegalStateException.class, () ->
            returnService.initiateReturn(orderId, changedBy));
        
        assertEquals("Return not allowed. Order must be DELIVERED.", ex.getMessage());

        ArgumentCaptor<StateHistory> historyCaptor = ArgumentCaptor.forClass(StateHistory.class);
        verify(stateHistoryRepository, times(1)).save(historyCaptor.capture());
        StateHistory history = historyCaptor.getValue();
        assertEquals("Order", history.getEntityType());
        assertEquals(orderId, history.getEntityId());
        assertEquals(order.getStatus().name(), history.getState());
        assertTrue(history.getNotes().contains("Return initiation rejected"));
    }

    @Test
    void testInitiateReturn_orderNotFoundThrows() {
        when(returnRepository.findByOrderId(orderId)).thenReturn(null);
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> returnService.initiateReturn(orderId, changedBy)
        );

        assertEquals("Order not found", ex.getMessage());
        verify(stateHistoryRepository, never()).save(any());
    }
    
    @Test
    void testTransitionReturnState_validNonCompletedTransition_logsAuditWithoutSchedulingRefund() {
        returnObj.setCreatedAt(Instant.now());
        when(returnRepository.findById(returnId)).thenReturn(Optional.of(returnObj));
        when(returnRepository.save(any(Return.class))).thenAnswer(i -> i.getArgument(0));
        
        Return result = returnService.transitionReturnState(returnId, ReturnStatus.APPROVED, changedBy);
        
        assertEquals(ReturnStatus.APPROVED, result.getStatus());
        // For non-COMPLETED transitions, audit is logged against the return's own id
        ArgumentCaptor<StateHistory> historyCaptor = ArgumentCaptor.forClass(StateHistory.class);
        verify(stateHistoryRepository, times(1)).save(historyCaptor.capture());
        StateHistory history = historyCaptor.getValue();
        assertEquals("Return", history.getEntityType());
        assertEquals(returnId, history.getEntityId());
        assertEquals(ReturnStatus.APPROVED.name(), history.getState());
        // Refund job should not be scheduled for non-COMPLETED transitions
        verify(jobScheduler, never()).scheduleRefundJob(any(), any());
    }
    
    @Test
    void testTransitionReturnState_toCompletedSchedulesRefundAndLogsAuditWithOrderTransaction() {
        // Set up a return that can transition to COMPLETED (e.g., from RECEIVED)
        returnObj.setStatus(ReturnStatus.RECEIVED);
        returnObj.setCreatedAt(Instant.now());
        when(returnRepository.findById(returnId)).thenReturn(Optional.of(returnObj));
        when(returnRepository.save(any(Return.class))).thenAnswer(i -> i.getArgument(0));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        Return result = returnService.transitionReturnState(returnId, ReturnStatus.COMPLETED, changedBy);

        assertEquals(ReturnStatus.COMPLETED, result.getStatus());
        verify(jobScheduler, times(1)).scheduleRefundJob(returnObj.getId(), orderId);

        ArgumentCaptor<StateHistory> historyCaptor = ArgumentCaptor.forClass(StateHistory.class);
        verify(stateHistoryRepository, times(1)).save(historyCaptor.capture());
        StateHistory history = historyCaptor.getValue();
        assertEquals("Return", history.getEntityType());
        assertEquals(returnId, history.getEntityId());
        // For COMPLETED, transactionId in the audit is the order's transactionId
        assertEquals(order.getTransactionId(), history.getTransactionId());
    }

    @Test
    void testTransitionReturnState_invalidTransitionReturnsNullAndDoesNotChangeOrSchedule() {
        returnObj.setStatus(ReturnStatus.REQUESTED);
        when(returnRepository.findById(returnId)).thenReturn(Optional.of(returnObj));
        
        // Try to transition from REQUESTED to COMPLETED (invalid transition)
        Return result = returnService.transitionReturnState(returnId, ReturnStatus.COMPLETED, changedBy);

        // For invalid transitions, service returns null so that the controller
        // can map this to a 422 INVALID_STATE_TRANSITION response, while the
        // underlying return entity remains unchanged.
        assertNull(result);
        assertEquals(ReturnStatus.REQUESTED, returnObj.getStatus());
        verify(returnRepository, never()).save(any(Return.class));
        verify(returnStateChangeRepository, never()).save(any());
        verify(jobScheduler, never()).scheduleRefundJob(any(), any());
        verify(stateHistoryRepository, never()).save(any());
    }

    @Test
    void testTransitionReturnState_returnNotFoundThrows() {
        when(returnRepository.findById(returnId)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> returnService.transitionReturnState(returnId, ReturnStatus.APPROVED, changedBy)
        );

        assertEquals("Return not found", ex.getMessage());
    }

    @Test
    void testTransitionReturnState_completedOrderNotFoundThrows() {
        returnObj.setStatus(ReturnStatus.RECEIVED);
        when(returnRepository.findById(returnId)).thenReturn(Optional.of(returnObj));
        when(returnRepository.save(any(Return.class))).thenAnswer(i -> i.getArgument(0));
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> returnService.transitionReturnState(returnId, ReturnStatus.COMPLETED, changedBy)
        );

        assertEquals("Order not found", ex.getMessage());
        // No refund job should be scheduled when the order cannot be loaded
        verify(jobScheduler, never()).scheduleRefundJob(any(), any());
    }

    @Test
    void testGetReturnStatus_success() {
        when(returnRepository.findById(returnId)).thenReturn(Optional.of(returnObj));

        ReturnStatus status = returnService.getReturnStatus(returnId);

        assertEquals(returnObj.getStatus(), status);
    }

    @Test
    void testGetReturnStatus_returnNotFoundThrows() {
        when(returnRepository.findById(returnId)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> returnService.getReturnStatus(returnId)
        );

        assertEquals("Return not found", ex.getMessage());
    }

    @Test
    void testGetReturnStateHistory_delegatesToRepository() {
        StateHistory h = new StateHistory();
        h.setEntityType("Return");
        h.setEntityId(returnId);
        List<StateHistory> histories = Collections.singletonList(h);
        when(stateHistoryRepository.findByEntityTypeAndEntityId("Return", returnId))
                .thenReturn(histories);

        List<StateHistory> result = returnService.getReturnStateHistory(returnId);

        assertEquals(histories, result);
        verify(stateHistoryRepository, times(1))
                .findByEntityTypeAndEntityId("Return", returnId);
    }
}
