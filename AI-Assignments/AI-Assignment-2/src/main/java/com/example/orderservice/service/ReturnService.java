package com.example.orderservice.service;

import java.util.List;
import java.util.UUID;

import com.example.orderservice.dto.OrderStatus;
import com.example.orderservice.dto.ReturnStatus;
import com.example.orderservice.entity.Return;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.ReturnStateChange;
import com.example.orderservice.entity.StateHistory;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.repository.ReturnRepository;
import com.example.orderservice.repository.ReturnStateChangeRepository;
import com.example.orderservice.repository.StateHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ReturnService {
    private final JobScheduler jobScheduler;
    private final ReturnStateChangeRepository returnStateChangeRepository;
    private final StateHistoryRepository stateHistoryRepository;
    private final ReturnRepository returnRepository;
    private final OrderRepository orderRepository;

    @Autowired
    public ReturnService(ReturnStateChangeRepository returnStateChangeRepository,
            OrderRepository orderRepository,
            ReturnRepository returnRepository,
            StateHistoryRepository stateHistoryRepository,
            JobScheduler jobScheduler) {
        this.returnStateChangeRepository = returnStateChangeRepository;
        this.orderRepository = orderRepository;
        this.returnRepository = returnRepository;
        this.stateHistoryRepository = stateHistoryRepository;
        this.jobScheduler = jobScheduler;
    }

    private boolean isEligibleForReturn(OrderStatus status) {
        return status.equals(OrderStatus.DELIVERED);
    }

    @Retryable(retryFor = TransientDataAccessException.class, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    @Transactional(rollbackFor = Exception.class)
    public Return initiateReturn(UUID orderId, UUID changedBy) {
        // Idempotency: if a return already exists for this order, treat this as
        // a safe retry from another device rather than creating a duplicate
        // return record. This keeps at most one active Return per order at the
        // application layer even if the database schema doesn't enforce a
        // uniqueness constraint on order_id.
        Return existingReturn = returnRepository.findByOrderId(orderId);
        if (existingReturn != null) {
            log.info("Returning existing Return {} for order {} (idempotent initiateReturn)", existingReturn.getId(),
                    orderId);
            return existingReturn;
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (!isEligibleForReturn(order.getStatus())) {
            // Even for rejected return requests, keep an audit entry tied to the
            // underlying order so that failed attempts are visible and
            // diagnosable from history. Use a monotonic event time relative to
            // the order's timestamps to keep audits readable under clock skew.
            Instant now = Instant.now();
            Instant baseline = order.getUpdatedAt() != null ? order.getUpdatedAt() : order.getCreatedAt();
            Instant eventTime = (baseline != null && now.isBefore(baseline)) ? baseline : now;

            StateHistory history = new StateHistory();
            history.setTransactionId(order.getTransactionId());
            // Log against the owning order using the same entityType
            // convention as OrderService.
            history.setEntityType("Order");
            history.setEntityId(order.getId());
            history.setState(order.getStatus().name());
            history.setChangedBy(changedBy);
            history.setTimestamp(eventTime);
            history.setCreatedAt(eventTime);
            history.setUpdatedAt(eventTime);
            history.setNotes("Return initiation rejected: order not in DELIVERED state.");
            stateHistoryRepository.save(history);

            log.warn("Return initiation rejected for order {} in state {}", orderId, order.getStatus());
            throw new IllegalStateException("Return not allowed. Order must be DELIVERED.");
        }

        // Use a single event time for all artifacts created as part of this
        // return initiation to keep audit timestamps consistent, even under
        // minor system clock adjustments.
        Instant eventTime = Instant.now();

        Return returnObj = new Return();
        returnObj.setId(UUID.randomUUID());
        returnObj.setOrderId(orderId);
        returnObj.setStatus(ReturnStatus.REQUESTED);
        returnObj.setCreatedAt(eventTime);
        returnRepository.save(returnObj);
        log.info("Return initiated with ID {} for order {}", returnObj.getId(), orderId);

        ReturnStateChange returnStateChange = new ReturnStateChange();
        returnStateChange.setId(UUID.randomUUID());
        returnStateChange.setReturnId(returnObj.getId());
        returnStateChange.setFromStatus(ReturnStatus.REQUESTED);
        returnStateChange.setToStatus(ReturnStatus.REQUESTED);
        returnStateChange.setUpdatedAt(eventTime);
        returnStateChange.setChangedBy(changedBy);
        returnStateChangeRepository.save(returnStateChange);

        logReturnAudit(returnObj, changedBy, order.getTransactionId(), eventTime);
        return returnObj;
    }

    @Retryable(retryFor = TransientDataAccessException.class, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    @Transactional(rollbackFor = Exception.class)
    public Return transitionReturnState(UUID returnId, ReturnStatus newState, UUID changedBy) {
        Return returnObj = returnRepository.findById(returnId)
                .orElseThrow(() -> new IllegalArgumentException("Return not found"));
        ReturnStatus current = returnObj.getStatus();
        ReturnStatus target = newState;
        boolean success = current.canTransitionTo(target);
        if (success) {
            // Keep timestamps for this transition monotonic relative to when the
            // return was created so audits remain readable even if the system
            // clock moves slightly backwards.
            Instant now = Instant.now();
            Instant baseline = returnObj.getCreatedAt();
            Instant eventTime = (baseline != null && now.isBefore(baseline)) ? baseline : now;

            returnObj.setStatus(target);
            returnRepository.save(returnObj);
            log.info("Return {} transitioned from {} to {} by {}", returnObj.getId(), current, target, changedBy);

            ReturnStateChange returnStateChange = new ReturnStateChange();
            returnStateChange.setId(UUID.randomUUID());
            returnStateChange.setReturnId(returnObj.getId());
            returnStateChange.setFromStatus(current);
            returnStateChange.setToStatus(target);
            returnStateChange.setUpdatedAt(eventTime);
            returnStateChange.setChangedBy(changedBy);
            returnStateChangeRepository.save(returnStateChange);

            // Trigger refund processing if state is COMPLETED
            if (target == ReturnStatus.COMPLETED) {
                Order order = orderRepository.findById(returnObj.getOrderId())
                        .orElseThrow(() -> new IllegalArgumentException("Order not found"));
                // Instead of invoking the refund gateway directly from the
                // state transition, schedule a refund job. This ensures that
                // refund processing is centralized, idempotent, and resilient
                // to races with later state changes or manual interventions.
                jobScheduler.scheduleRefundJob(returnObj.getId(), order.getId());
                // Log that the return reached COMPLETED and a refund job was
                // scheduled, using the order's transactionId for consistency.
                logReturnAudit(returnObj, changedBy, order.getTransactionId(), eventTime);
                log.info("Scheduled refund job for completed return {} and order {}", returnObj.getId(),
                        order.getId());
            } else {
                // For non-completed transitions, log against the return's own id
                logReturnAudit(returnObj, changedBy, returnObj.getId(), eventTime);
            }
        } else {
            log.warn("Invalid return state transition attempted for return {} from {} to {}", returnObj.getId(),
                    current, target);
            return null;
        }
        return returnObj;
    }

    public ReturnStatus getReturnStatus(UUID returnId) {
        Return returnObj = returnRepository.findById(returnId)
                .orElseThrow(() -> new IllegalArgumentException("Return not found"));
        return returnObj.getStatus();
    }

    public List<StateHistory> getReturnStateHistory(UUID returnId) {
        return stateHistoryRepository.findByEntityTypeAndEntityId("Return", returnId);
    }

    private void logReturnAudit(Return returnObj, UUID changedBy, UUID transactionId, Instant eventTime) {
        StateHistory history = new StateHistory();
        history.setTransactionId(transactionId);
        history.setEntityType("Return");
        history.setEntityId(returnObj.getId());
        history.setState(returnObj.getStatus().name());
        history.setChangedBy(changedBy);
        history.setTimestamp(eventTime);
        history.setCreatedAt(eventTime);
        history.setUpdatedAt(eventTime);
        stateHistoryRepository.save(history);
    }
}
