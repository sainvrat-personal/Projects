package com.example.orderservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.example.orderservice.dto.RefundRequest;
import com.example.orderservice.dto.RefundResponse;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.Return;
import com.example.orderservice.entity.StateHistory;
import com.example.orderservice.repository.StateHistoryRepository;

import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Service
@Slf4j
public class RefundJobService {
    private final StateHistoryRepository stateHistoryRepository;
    private final RestTemplate restTemplate;
    private static final String MOCK_PAYMENT_URL = "http://localhost:8080/mock-payment/refund";

    /**
     * Optional bearer token used to authenticate with the payment gateway.
     * In production this would typically be a short-lived OAuth/JWT token or
     * API key; here we keep it simple and configurable.
     */
    @Value("${payment.gateway.auth-token:}")
    private String paymentGatewayAuthToken;

    @Autowired
    public RefundJobService(StateHistoryRepository stateHistoryRepository, RestTemplate restTemplate) {
        this.stateHistoryRepository = stateHistoryRepository;
        this.restTemplate = restTemplate;
    }

    /**
     * Asynchronous entry-point used by existing flows.
     * <p>
     * This method runs on the dedicated {@code refundJobExecutor} thread-pool
     * so that potentially slow payment-gateway calls are isolated from other
     * async work. It delegates to the synchronous implementation so that job
     * scheduler callers can get proper error propagation.
     */
    @Async("refundJobExecutor")
    public void processRefundAsync(Return returnObj, Order order, UUID changedBy) {
        try {
            processRefund(returnObj, order, changedBy);
        } catch (IllegalArgumentException e) {
            // For validation and configuration issues (e.g., invalid amount or
            // mismatched return/order), log a clear warning and treat this as
            // a one-off failure for the async caller. The core processRefund
            // method has already written an appropriate audit entry.
            log.warn("Skipping async refund for return {} / order {}: {}",
                    returnObj.getId(), order.getId(), e.getMessage());
        } catch (Exception e) {
            // For connection-refused, timeouts, or other technical failures,
            // log a detailed error so operators can correlate with gateway
            // availability issues. The job-based path still uses synchronous
            // processing with retries and audit logging.
            log.error("Async refund processing failed for return {} / order {}: {}",
                    returnObj.getId(), order.getId(), e.getMessage(), e);
        }
    }

    /**
     * Synchronous refund processing used by the job scheduler.
     * Any non-successful refund (HTTP error, network failure, business failure)
     * will throw a RuntimeException so that the job can be marked as FAILED/RETRY.
     */
    public void processRefund(Return returnObj, Order order, UUID changedBy) {
        log.info("[RefundJobService] Processing refund for return: {}, orderId: {}, amount: {}",
                returnObj.getId(), order.getId(), order.getPrice());

        // Idempotency guard: if we have already recorded a successful refund
        // for this specific return, treat this invocation as a no-op. This
        // prevents duplicate refunds when jobs are retried or triggered
        // manually.
        if (hasExistingSuccessfulRefund(returnObj)) {
            log.info("[RefundJobService] Skipping refund for return {} / order {} because a prior refund "
                    + "with state REFUNDED already exists.", returnObj.getId(), order.getId());
            return;
        }

        // Defensive check: ensure the provided Return really belongs to the
        // provided Order. This protects against programming errors or stale
        // data where the job pipeline might accidentally combine mismatched
        // entities, which would otherwise produce confusing audit trails.
        if (returnObj.getOrderId() != null && !returnObj.getOrderId().equals(order.getId())) {
            String message = "Return/order mismatch: return " + returnObj.getId()
                    + " is linked to order " + returnObj.getOrderId()
                    + " but refund was invoked with order " + order.getId();
            log.error(message);
            logRefundAudit(returnObj, order, changedBy, "REFUND_FAILED", message);
            throw new IllegalArgumentException(message);
        }

        // Calculate total refund amount (price * quantity)
        double refundAmount = order.getPrice() * order.getQuantity();

        // Basic validation – avoid calling the gateway with clearly invalid
        // monetary values. Although upstream flows should prevent such cases,
        // we keep this as a final safety net.
        if (refundAmount <= 0) {
            String message = "Refund amount must be positive for order " + order.getId() + " (computed=" + refundAmount
                    + ")";
            log.error(message);
            logRefundAudit(returnObj, order, changedBy, "REFUND_FAILED", message);
            throw new IllegalArgumentException(message);
        }

        // Create the request payload
        RefundRequest request = new RefundRequest();
        request.setReturnId(returnObj.getId().toString());
        request.setOrderId(order.getId().toString());
        request.setTransactionId(order.getTransactionId().toString());
        request.setAmount(refundAmount);
        request.setCustomerEmail(order.getEmail());
        request.setReason("Customer return - approved");

        // Validate the outbound RefundRequest payload before calling the
        // mock payment gateway so that we never send structurally invalid
        // content (missing IDs, zero/negative amounts, empty email, etc.).
        String validationError = validateRefundRequest(request);
        if (validationError != null) {
            log.error("Invalid RefundRequest for order {} / return {}: {}", order.getId(), returnObj.getId(),
                    validationError);
            logRefundAudit(returnObj, order, changedBy, "REFUND_FAILED",
                    "Invalid refund request: " + validationError);
            throw new IllegalArgumentException("Invalid refund request: " + validationError);
        }

        // Derive a deterministic idempotency reference for this refund
        // attempt that can be used when reconciling with the payment
        // gateway in the face of partial failures (e.g., timeouts where
        // the gateway may or may not have processed the refund).
        String refundIdempotencyRef = order.getTransactionId() + ":" + returnObj.getId();

        try {
            // Build HTTP headers including optional Authorization for the
            // payment gateway. If no token is configured, we still attempt
            // the call; any resulting 401/403 will be handled explicitly
            // below and surfaced as REFUND_AUTH_ERROR in the audit log.
            HttpHeaders headers = new HttpHeaders();
            if (paymentGatewayAuthToken != null && !paymentGatewayAuthToken.trim().isEmpty()) {
                headers.setBearerAuth(paymentGatewayAuthToken.trim());
            }

            HttpEntity<RefundRequest> httpEntity = new HttpEntity<>(request, headers);

            // Make the API call to the mock payment service
            ResponseEntity<RefundResponse> response = restTemplate.postForEntity(
                    MOCK_PAYMENT_URL,
                    httpEntity,
                    RefundResponse.class);

            RefundResponse refundResponse = response.getBody();
            if (response.getStatusCode() == HttpStatus.OK && refundResponse != null && refundResponse.isSuccess()) {
                log.info("Refund successful for order {}: {}, refundId={}",
                        order.getId(), refundResponse.getMessage(), refundResponse.getRefundId());

                // Log successful refund audit entry
                String auditMessage = String.format(
                        "Refund processed successfully. Amount: $%.2f, Refund ID: %s, Transaction ID: %s, IdempotencyRef: %s",
                        refundResponse.getAmount(),
                        refundResponse.getRefundId(),
                        refundResponse.getTransactionId(),
                        refundIdempotencyRef);

                logRefundAudit(returnObj, order, changedBy, "REFUNDED", auditMessage);
                // Here you could also update the return status or send an email notification
                return;
            }

            String errorMessage = (refundResponse != null)
                    ? refundResponse.getMessage()
                    : "Unknown error from payment gateway";

            // Gateway-specific validation failures (amount too high, currency
            // mismatch, business rule violations, etc.) are communicated as a
            // non-success response with HTTP 200. Treat these as definitive,
            // non-retryable business failures: record them distinctly in the
            // audit log and allow the job to complete without retries so that
            // operators can decide on manual follow-up if needed.
            log.warn("Refund declined by gateway for order {} / return {}: {}",
                    order.getId(), returnObj.getId(), errorMessage);
            logRefundAudit(returnObj, order, changedBy, "REFUND_DECLINED",
                    "Refund declined by gateway: " + errorMessage
                            + " [IdempotencyRef=" + refundIdempotencyRef + "]");

            // Do not throw here; JobScheduler will still mark the job as
            // COMPLETED, while the audit trail clearly shows the refund was
            // declined rather than technically processed.
            return;
        } catch (ResourceAccessException e) {
            String message = "Network error while calling payment gateway: " + e.getMessage();
            log.error(message, e);
            // The outcome at the gateway is now uncertain: the request may
            // have been processed even though our side observed a timeout or
            // connection failure. Record this as an UNKNOWN outcome and
            // include a deterministic idempotency reference so that a future
            // reconciliation job or operator can query the gateway by this
            // key.
            logRefundAudit(returnObj, order, changedBy, "REFUND_STATUS_UNKNOWN",
                    "Network error during refund processing (outcome unknown). "
                            + "IdempotencyRef=" + refundIdempotencyRef + "; details=" + message);
            throw new RuntimeException(message, e);
        } catch (HttpStatusCodeException e) {
            int statusCode = e.getStatusCode().value();
            String body = e.getResponseBodyAsString();
            String message = "HTTP " + statusCode + " from payment gateway: " + body;
            log.error(message, e);

            // Treat authentication/authorization failures as non-transient
            // configuration problems so they are clearly distinguishable in
            // the audit log from generic transient HTTP errors.
            if (statusCode == 401 || statusCode == 403) {
                String authMsg = "Payment gateway authentication/authorization failed. "
                        + "Check API credentials or tokens. "
                        + "IdempotencyRef=" + refundIdempotencyRef + "; details=" + message;
                logRefundAudit(returnObj, order, changedBy, "REFUND_AUTH_ERROR", authMsg);
                throw new IllegalStateException(authMsg, e);
            }

            // For 429 (rate limiting / throttling), keep this classified as a
            // transient, retryable condition but mark it distinctly in the
            // audit log so operators can see that the gateway is throttling.
            if (statusCode == 429) {
                String rateMsg = "Payment gateway rate limit exceeded (HTTP 429). "
                        + "IdempotencyRef=" + refundIdempotencyRef + "; details=" + message;
                logRefundAudit(returnObj, order, changedBy, "REFUND_RATE_LIMITED", rateMsg);
                throw new RuntimeException(rateMsg, e);
            }

            logRefundAudit(returnObj, order, changedBy, "REFUND_STATUS_UNKNOWN",
                    "HTTP error during refund processing (outcome unknown). "
                            + "IdempotencyRef=" + refundIdempotencyRef + "; details=" + message);
            throw new RuntimeException(message, e);
        } catch (Exception e) {
            String message = "Unexpected error during refund processing: " + e.getMessage();
            log.error(message, e);
            logRefundAudit(returnObj, order, changedBy, "REFUND_STATUS_UNKNOWN",
                    "Unexpected error during refund processing (outcome unknown). "
                            + "IdempotencyRef=" + refundIdempotencyRef + "; details=" + message);
            throw new RuntimeException(message, e);
        }
    }

    /**
     * Perform lightweight structural validation of the outbound RefundRequest
     * before sending it to the mock payment gateway.
     *
     * @return null if valid; otherwise a human-readable error description.
     */
    private String validateRefundRequest(RefundRequest request) {
        if (request.getReturnId() == null || request.getReturnId().trim().isEmpty()) {
            return "returnId is required";
        }
        if (request.getOrderId() == null || request.getOrderId().trim().isEmpty()) {
            return "orderId is required";
        }
        if (request.getTransactionId() == null || request.getTransactionId().trim().isEmpty()) {
            return "transactionId is required";
        }
        if (request.getAmount() == null) {
            return "amount is required";
        }
        if (request.getAmount() <= 0) {
            return "amount must be greater than zero";
        }
        String email = request.getCustomerEmail();
        if (email == null || email.trim().isEmpty()) {
            return "customerEmail is required";
        }
        // Basic email format validation
        if (!email.trim().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            return "customerEmail format is invalid";
        }
        return null;
    }

    private void logRefundAudit(Return returnObj, Order order, UUID changedBy, String status, String message) {
        StateHistory history = new StateHistory();
        history.setTransactionId(order.getTransactionId());
        history.setEntityType("Refund");
        // Use the return's id as the entityId so that refund idempotency
        // checks can be scoped cleanly per-return.
        history.setEntityId(returnObj.getId());
        history.setState(status);
        history.setChangedBy(changedBy);
        history.setTimestamp(java.time.Instant.now());
        history.setCreatedAt(java.time.Instant.now());
        history.setUpdatedAt(java.time.Instant.now());
        history.setNotes(message); // Store response or error message
        stateHistoryRepository.save(history);
    }

    private boolean hasExistingSuccessfulRefund(Return returnObj) {
        return !stateHistoryRepository
                .findByEntityTypeAndEntityIdAndState("Refund", returnObj.getId(), "REFUNDED")
                .isEmpty();
    }
}
