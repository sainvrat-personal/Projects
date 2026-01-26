package com.example.orderservice.service;

import com.example.orderservice.dto.CreateOrderRequest;
import com.example.orderservice.dto.CreateOrderRequest.Item;
import com.example.orderservice.dto.OrderStatus;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.StateHistory;
import com.example.orderservice.exception.ResourceNotFoundException;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.repository.StateHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OrderService {
    private final OrderRepository orderRepository;
    private final StateHistoryRepository stateHistoryRepository;
    private final InvoiceJobService invoiceJobService;
    private final EmailService emailService;
    private final JobScheduler jobScheduler;

    // Valid transitions map
    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = new EnumMap<>(OrderStatus.class);
    static {
        VALID_TRANSITIONS.put(OrderStatus.PENDING_PAYMENT,
                new HashSet<>(Arrays.asList(OrderStatus.PAID, OrderStatus.CANCELLED)));
        VALID_TRANSITIONS.put(OrderStatus.PAID, new HashSet<>(
                Arrays.asList(OrderStatus.PROCESSING_IN_WAREHOUSE, OrderStatus.SHIPPED, OrderStatus.CANCELLED)));
        VALID_TRANSITIONS.put(OrderStatus.PROCESSING_IN_WAREHOUSE, new HashSet<>(Arrays.asList(OrderStatus.SHIPPED)));
        VALID_TRANSITIONS.put(OrderStatus.SHIPPED, new HashSet<>(Arrays.asList(OrderStatus.DELIVERED)));
        VALID_TRANSITIONS.put(OrderStatus.DELIVERED, new HashSet<>());
        VALID_TRANSITIONS.put(OrderStatus.CANCELLED, new HashSet<>());
    }

    @Autowired
    public OrderService(OrderRepository orderRepository, StateHistoryRepository stateHistoryRepository,
            InvoiceJobService invoiceJobService, EmailService emailService, JobScheduler jobScheduler) {
        this.orderRepository = orderRepository;
        this.stateHistoryRepository = stateHistoryRepository;
        this.invoiceJobService = invoiceJobService;
        this.emailService = emailService;
        this.jobScheduler = jobScheduler;
    }

    @Retryable(retryFor = TransientDataAccessException.class, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        Item item = request.getItem();
        log.info("Creating order for customerId={} transactionId={}", request.getCustomerId(),
                request.getTransactionId());
        if (request.getCustomerId() == null) {
            throw new IllegalArgumentException("Customer ID is required.");
        }
        if (item == null) {
            throw new IllegalArgumentException("Order must have at least one item.");
        }
        if (item.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
        if (item.getPrice() <= 0) {
            throw new IllegalArgumentException("Price must be greater than zero.");
        }
        if (request.getShippingAddress() == null || request.getShippingAddress().trim().isEmpty()) {
            throw new IllegalArgumentException("Shipping address is required.");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required.");
        }
        String email = request.getEmail().trim();
        // Basic email format validation (user@domain.tld)
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("Email format is invalid.");
        }
        // Idempotency: if client supplies a transactionId and an order already
        // exists for it, treat this as a safe retry and return the existing order
        if (request.getTransactionId() != null) {
            return orderRepository.findByTransactionId(request.getTransactionId())
                    .orElseGet(() -> createAndPersistOrder(request, item));
        }

        // No client-supplied transactionId – create a fresh order
        return createAndPersistOrder(request, item);
    }

    private Order createAndPersistOrder(CreateOrderRequest request, Item item) {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setTransactionId(request.getTransactionId() != null ? request.getTransactionId() : UUID.randomUUID());
        order.setEmail(request.getEmail());
        order.setCustomerId(request.getCustomerId());
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setShippingAddress(request.getShippingAddress());
        order.setProductId(item.getProductId());
        order.setQuantity(item.getQuantity());
        order.setPrice(item.getPrice());

        // Use a single, monotonic event time for creation to avoid clock-skew
        // inconsistencies between createdAt, updatedAt, and the first audit entry.
        Instant now = Instant.now();
        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        orderRepository.save(order);
        logStateChange(order, OrderStatus.PENDING_PAYMENT, request.getCustomerId(), now);
        log.info("Order created: id={} status={} customerId={}", order.getId(), order.getStatus(),
                order.getCustomerId());
        return order;
    }

    @Retryable(retryFor = TransientDataAccessException.class, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    @Transactional
    public Order transitionOrderState(UUID orderId, OrderStatus newState, UUID changedBy) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (!VALID_TRANSITIONS.get(order.getStatus()).contains(newState)) {
            log.warn("Invalid order state transition attempt for order {} from {} to {}", orderId, order.getStatus(),
                    newState);
            throw new IllegalStateException("Invalid state transition from " + order.getStatus() + " to " + newState);
        }

        // Derive a monotonic event time so that updatedAt never moves backwards
        // relative to previous updates for this order, even if the system clock
        // skews backwards.
        Instant now = Instant.now();
        Instant baseline = order.getUpdatedAt() != null ? order.getUpdatedAt() : order.getCreatedAt();
        Instant eventTime = (baseline != null && now.isBefore(baseline)) ? baseline : now;

        order.setStatus(newState);
        order.setUpdatedAt(eventTime);
        orderRepository.save(order);
        if (newState == OrderStatus.SHIPPED) {
            // Trigger an idempotent background invoice job instead of firing
            // the async generator directly. The JobScheduler enforces
            // idempotency so that multiple SHIPPED events for the same order
            // do not result in duplicate invoices.
            jobScheduler.scheduleInvoiceJob(order.getId());
        }
        logStateChange(order, newState, changedBy, eventTime);
        log.info("Order {} transitioned to {} by {}", orderId, newState, changedBy);
        return order;
    }

    /**
     * Cancel an order if it is in PENDING_PAYMENT or PAID state.
     * Logs the cancellation in the audit trail.
     */
    @Retryable(retryFor = TransientDataAccessException.class, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    @Transactional(rollbackFor = Exception.class)
    public Order cancelOrder(UUID orderId, String reason, UUID changedBy) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        // Idempotency: if the order is already CANCELLED, treat this as a
        // successful no-op rather than an error so that duplicate cancellation
        // requests from multiple devices don't surface as failures.
        if (order.getStatus() == OrderStatus.CANCELLED) {
            log.info("Cancellation requested for already cancelled order {}; treating as idempotent success", orderId);
            return order;
        }

        // Require a non-empty cancellation reason for auditability. We enforce
        // this only when performing a real state change; idempotent repeats of
        // an already-cancelled order are allowed even if the client omits the
        // reason on subsequent calls.
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Cancellation reason is required.");
        }

        // Keep cancellation timestamps monotonic with prior order updates, even
        // when a cancellation attempt is rejected due to an invalid state.
        Instant now = Instant.now();
        Instant baseline = order.getUpdatedAt() != null ? order.getUpdatedAt() : order.getCreatedAt();
        Instant eventTime = (baseline != null && now.isBefore(baseline)) ? baseline : now;

        // For invalid cancellation states, record an audit entry with the
        // current state so that failed attempts are still visible in the
        // history, then surface a clear business error.
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT && order.getStatus() != OrderStatus.PAID) {
            logStateChange(order, order.getStatus(), changedBy, eventTime);
            log.warn("Cancellation rejected for order {} in state {}", orderId, order.getStatus());
            throw new IllegalStateException("Only orders in PENDING_PAYMENT or PAID state can be cancelled");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setFailureReason(reason);
        order.setUpdatedAt(eventTime);
        orderRepository.save(order);
        logStateChange(order, OrderStatus.CANCELLED, changedBy, eventTime);

        log.info("Order {} cancelled by {} with reason '{}'", orderId, changedBy, reason);
        return order;
    }

    private void logStateChange(Order order, OrderStatus newState, UUID changedBy, Instant eventTime) {
        StateHistory history = new StateHistory();
        history.setTransactionId(order.getTransactionId());
        // Use a consistent, title-cased entityType so that queries and audits
        // behave predictably across the system.
        history.setEntityType("Order");
        history.setEntityId(order.getId());
        history.setState(newState.name());
        history.setChangedBy(changedBy);
        history.setTimestamp(eventTime);
        history.setCreatedAt(eventTime);
        history.setUpdatedAt(eventTime);
        history.setNotes(order.getFailureReason());
        stateHistoryRepository.save(history);
    }

    public List<StateHistory> getOrderStateHistory(UUID orderId) {
        // Must match the entityType used when logging order state changes.
        return stateHistoryRepository.findByEntityTypeAndEntityId("Order", orderId);
    }
}
