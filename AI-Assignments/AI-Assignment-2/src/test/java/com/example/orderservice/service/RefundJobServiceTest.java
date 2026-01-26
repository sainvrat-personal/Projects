package com.example.orderservice.service;

import com.example.orderservice.dto.RefundRequest;
import com.example.orderservice.dto.RefundResponse;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.Return;
import com.example.orderservice.entity.StateHistory;
import com.example.orderservice.repository.StateHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RefundJobServiceTest {

    @Mock
    private StateHistoryRepository stateHistoryRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private RefundJobService refundJobService;

    private UUID orderId;
    private UUID returnId;
    private UUID changedBy;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        orderId = UUID.randomUUID();
        returnId = UUID.randomUUID();
        changedBy = UUID.randomUUID();
    }

    private Order newOrder() {
        Order order = new Order();
        order.setId(orderId);
        order.setTransactionId(UUID.randomUUID());
        order.setCustomerId(UUID.randomUUID());
        order.setShippingAddress("123 Test Street");
        order.setEmail("customer@example.com");
        order.setProductId(UUID.randomUUID());
        order.setQuantity(2);
        order.setPrice(10.0);
        return order;
    }

    private Return newReturn(UUID orderIdForReturn) {
        Return r = new Return();
        r.setId(returnId);
        r.setOrderId(orderIdForReturn);
        return r;
    }

    private void setPaymentGatewayAuthToken(String token) throws Exception {
        Field f = RefundJobService.class.getDeclaredField("paymentGatewayAuthToken");
        f.setAccessible(true);
        f.set(refundJobService, token);
    }

    // ---------------------------------------------------------------------
    // processRefundAsync
    // ---------------------------------------------------------------------

    @Test
    void processRefundAsync_delegatesAndSwallowsIllegalArgumentException() {
        RefundJobService spyService = spy(refundJobService);
        Order order = newOrder();
        Return ret = newReturn(orderId);

        doThrow(new IllegalArgumentException("validation error"))
                .when(spyService)
                .processRefund(ret, order, changedBy);

        assertDoesNotThrow(() -> spyService.processRefundAsync(ret, order, changedBy));
        verify(spyService).processRefund(ret, order, changedBy);
    }

    @Test
    void processRefundAsync_delegatesAndSwallowsUnexpectedException() {
        RefundJobService spyService = spy(refundJobService);
        Order order = newOrder();
        Return ret = newReturn(orderId);

        doThrow(new RuntimeException("boom"))
                .when(spyService)
                .processRefund(ret, order, changedBy);

        assertDoesNotThrow(() -> spyService.processRefundAsync(ret, order, changedBy));
        verify(spyService).processRefund(ret, order, changedBy);
    }

    @Test
    void processRefundAsync_delegatesSuccessfully() {
        RefundJobService spyService = spy(refundJobService);
        Order order = newOrder();
        Return ret = newReturn(orderId);

        doNothing().when(spyService).processRefund(ret, order, changedBy);

        assertDoesNotThrow(() -> spyService.processRefundAsync(ret, order, changedBy));
        verify(spyService).processRefund(ret, order, changedBy);
    }

    // ---------------------------------------------------------------------
    // processRefund - idempotency and basic validation
    // ---------------------------------------------------------------------

    @Test
    void processRefund_skipsWhenExistingSuccessfulRefund() {
        Order order = newOrder();
        Return ret = newReturn(orderId);

        List<StateHistory> existing = new ArrayList<>();
        existing.add(new StateHistory());
        when(stateHistoryRepository.findByEntityTypeAndEntityIdAndState("Refund", ret.getId(), "REFUNDED"))
                .thenReturn(existing);

        refundJobService.processRefund(ret, order, changedBy);

        verify(stateHistoryRepository, times(1))
                .findByEntityTypeAndEntityIdAndState("Refund", ret.getId(), "REFUNDED");
        verifyNoInteractions(restTemplate);
        verify(stateHistoryRepository, times(0)).save(any(StateHistory.class));
    }

    @Test
    void processRefund_throwsWhenReturnOrderMismatch() {
        Order order = newOrder();
        // Mismatched order id on return
        Return ret = newReturn(UUID.randomUUID());

        when(stateHistoryRepository.findByEntityTypeAndEntityIdAndState("Refund", ret.getId(), "REFUNDED"))
                .thenReturn(Collections.emptyList());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> refundJobService.processRefund(ret, order, changedBy)
        );

        assertTrue(ex.getMessage().contains("Return/order mismatch"));

        ArgumentCaptor<StateHistory> captor = ArgumentCaptor.forClass(StateHistory.class);
        verify(stateHistoryRepository).save(captor.capture());
        StateHistory history = captor.getValue();
        assertEquals("Refund", history.getEntityType());
        assertEquals("REFUND_FAILED", history.getState());
        assertNotNull(history.getNotes());
    }

    @Test
    void processRefund_throwsWhenRefundAmountNonPositive() {
        Order order = newOrder();
        order.setPrice(0.0); // makes refundAmount <= 0
        Return ret = newReturn(orderId);

        when(stateHistoryRepository.findByEntityTypeAndEntityIdAndState("Refund", ret.getId(), "REFUNDED"))
                .thenReturn(Collections.emptyList());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> refundJobService.processRefund(ret, order, changedBy)
        );

        assertTrue(ex.getMessage().contains("Refund amount must be positive"));

        ArgumentCaptor<StateHistory> captor = ArgumentCaptor.forClass(StateHistory.class);
        verify(stateHistoryRepository).save(captor.capture());
        assertEquals("REFUND_FAILED", captor.getValue().getState());
    }

    @Test
    void processRefund_throwsWhenCustomerEmailMissing() {
        Order order = newOrder();
        order.setEmail(null);
        Return ret = newReturn(orderId);

        when(stateHistoryRepository.findByEntityTypeAndEntityIdAndState("Refund", ret.getId(), "REFUNDED"))
                .thenReturn(Collections.emptyList());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> refundJobService.processRefund(ret, order, changedBy)
        );

        assertTrue(ex.getMessage().contains("Invalid refund request: customerEmail is required"));

        ArgumentCaptor<StateHistory> captor = ArgumentCaptor.forClass(StateHistory.class);
        verify(stateHistoryRepository).save(captor.capture());
        assertEquals("REFUND_FAILED", captor.getValue().getState());
    }

    @Test
    void processRefund_throwsWhenCustomerEmailInvalidFormat() {
        Order order = newOrder();
        order.setEmail("not-an-email");
        Return ret = newReturn(orderId);

        when(stateHistoryRepository.findByEntityTypeAndEntityIdAndState("Refund", ret.getId(), "REFUNDED"))
                .thenReturn(Collections.emptyList());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> refundJobService.processRefund(ret, order, changedBy)
        );

        assertTrue(ex.getMessage().contains("Invalid refund request: customerEmail format is invalid"));

        ArgumentCaptor<StateHistory> captor = ArgumentCaptor.forClass(StateHistory.class);
        verify(stateHistoryRepository).save(captor.capture());
        assertEquals("REFUND_FAILED", captor.getValue().getState());
    }

    // ---------------------------------------------------------------------
    // processRefund - happy path and gateway responses
    // ---------------------------------------------------------------------

    @Test
    void processRefund_successfulRefundLogsAuditAndReturns() throws Exception {
        Order order = newOrder();
        Return ret = newReturn(orderId);

        when(stateHistoryRepository.findByEntityTypeAndEntityIdAndState("Refund", ret.getId(), "REFUNDED"))
                .thenReturn(Collections.emptyList());

        setPaymentGatewayAuthToken("  bearer-token  ");

        RefundResponse body = new RefundResponse(
                true,
                "OK",
                order.getTransactionId().toString(),
                "refund-123",
                order.getPrice() * order.getQuantity(),
                "SUCCESS",
                "2024-01-01T00:00:00Z"
        );

        when(restTemplate.postForEntity(
                eq("http://localhost:8080/mock-payment/refund"),
                any(HttpEntity.class),
                eq(RefundResponse.class)))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        refundJobService.processRefund(ret, order, changedBy);

        ArgumentCaptor<StateHistory> captor = ArgumentCaptor.forClass(StateHistory.class);
        verify(stateHistoryRepository).save(captor.capture());
        StateHistory history = captor.getValue();
        assertEquals("Refund", history.getEntityType());
        assertEquals(ret.getId(), history.getEntityId());
        assertEquals("REFUNDED", history.getState());
        assertNotNull(history.getNotes());

        verify(restTemplate).postForEntity(
                eq("http://localhost:8080/mock-payment/refund"),
                any(HttpEntity.class),
                eq(RefundResponse.class));
    }

    @Test
    void processRefund_gatewayDeclinesRefund_withMessage() {
        Order order = newOrder();
        Return ret = newReturn(orderId);

        when(stateHistoryRepository.findByEntityTypeAndEntityIdAndState("Refund", ret.getId(), "REFUNDED"))
                .thenReturn(Collections.emptyList());

        RefundResponse body = new RefundResponse(
                false,
                "Insufficient funds",
                order.getTransactionId().toString(),
                null,
                order.getPrice() * order.getQuantity(),
                "DECLINED",
                "2024-01-01T00:00:00Z"
        );

        when(restTemplate.postForEntity(
                eq("http://localhost:8080/mock-payment/refund"),
                any(HttpEntity.class),
                eq(RefundResponse.class)))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        refundJobService.processRefund(ret, order, changedBy);

        ArgumentCaptor<StateHistory> captor = ArgumentCaptor.forClass(StateHistory.class);
        verify(stateHistoryRepository).save(captor.capture());
        assertEquals("REFUND_DECLINED", captor.getValue().getState());
    }

    @Test
    void processRefund_gatewayDeclinesRefund_withNullBody() {
        Order order = newOrder();
        Return ret = newReturn(orderId);

        when(stateHistoryRepository.findByEntityTypeAndEntityIdAndState("Refund", ret.getId(), "REFUNDED"))
                .thenReturn(Collections.emptyList());

        when(restTemplate.postForEntity(
                eq("http://localhost:8080/mock-payment/refund"),
                any(HttpEntity.class),
                eq(RefundResponse.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        refundJobService.processRefund(ret, order, changedBy);

        ArgumentCaptor<StateHistory> captor = ArgumentCaptor.forClass(StateHistory.class);
        verify(stateHistoryRepository).save(captor.capture());
        assertEquals("REFUND_DECLINED", captor.getValue().getState());
    }

    // ---------------------------------------------------------------------
    // processRefund - exception handling around RestTemplate
    // ---------------------------------------------------------------------

    @Test
    void processRefund_throwsRuntimeOnNetworkErrorAndLogsUnknownStatus() {
        Order order = newOrder();
        Return ret = newReturn(orderId);

        when(stateHistoryRepository.findByEntityTypeAndEntityIdAndState("Refund", ret.getId(), "REFUNDED"))
                .thenReturn(Collections.emptyList());

        when(restTemplate.postForEntity(
                eq("http://localhost:8080/mock-payment/refund"),
                any(HttpEntity.class),
                eq(RefundResponse.class)))
                .thenThrow(new ResourceAccessException("timeout"));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> refundJobService.processRefund(ret, order, changedBy)
        );

        assertTrue(ex.getMessage().contains("Network error while calling payment gateway"));

        ArgumentCaptor<StateHistory> captor = ArgumentCaptor.forClass(StateHistory.class);
        verify(stateHistoryRepository).save(captor.capture());
        assertEquals("REFUND_STATUS_UNKNOWN", captor.getValue().getState());
    }

    @Test
    void processRefund_throwsIllegalStateOnAuthError() {
        Order order = newOrder();
        Return ret = newReturn(orderId);

        when(stateHistoryRepository.findByEntityTypeAndEntityIdAndState("Refund", ret.getId(), "REFUNDED"))
                .thenReturn(Collections.emptyList());

        HttpStatusCodeException ex401 = HttpClientErrorException.create(
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                HttpHeaders.EMPTY,
                "unauthorized".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);

        when(restTemplate.postForEntity(
                eq("http://localhost:8080/mock-payment/refund"),
                any(HttpEntity.class),
                eq(RefundResponse.class)))
                .thenThrow(ex401);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> refundJobService.processRefund(ret, order, changedBy)
        );

        assertTrue(ex.getMessage().contains("Payment gateway authentication/authorization failed"));

        ArgumentCaptor<StateHistory> captor = ArgumentCaptor.forClass(StateHistory.class);
        verify(stateHistoryRepository).save(captor.capture());
        assertEquals("REFUND_AUTH_ERROR", captor.getValue().getState());
    }

    @Test
    void processRefund_throwsRuntimeOnRateLimitError() {
        Order order = newOrder();
        Return ret = newReturn(orderId);

        when(stateHistoryRepository.findByEntityTypeAndEntityIdAndState("Refund", ret.getId(), "REFUNDED"))
                .thenReturn(Collections.emptyList());

        HttpStatusCodeException ex429 = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too Many Requests",
                HttpHeaders.EMPTY,
                "rate limited".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);

        when(restTemplate.postForEntity(
                eq("http://localhost:8080/mock-payment/refund"),
                any(HttpEntity.class),
                eq(RefundResponse.class)))
                .thenThrow(ex429);

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> refundJobService.processRefund(ret, order, changedBy)
        );

        assertTrue(ex.getMessage().contains("Payment gateway rate limit exceeded"));

        ArgumentCaptor<StateHistory> captor = ArgumentCaptor.forClass(StateHistory.class);
        verify(stateHistoryRepository).save(captor.capture());
        assertEquals("REFUND_RATE_LIMITED", captor.getValue().getState());
    }

    @Test
    void processRefund_throwsRuntimeOnOtherHttpError() {
        Order order = newOrder();
        Return ret = newReturn(orderId);

        when(stateHistoryRepository.findByEntityTypeAndEntityIdAndState("Refund", ret.getId(), "REFUNDED"))
                .thenReturn(Collections.emptyList());

        HttpStatusCodeException ex500 = HttpServerErrorException.create(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                HttpHeaders.EMPTY,
                "server error".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);

        when(restTemplate.postForEntity(
                eq("http://localhost:8080/mock-payment/refund"),
                any(HttpEntity.class),
                eq(RefundResponse.class)))
                .thenThrow(ex500);

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> refundJobService.processRefund(ret, order, changedBy)
        );

        assertTrue(ex.getMessage().contains("HTTP 500"));

        ArgumentCaptor<StateHistory> captor = ArgumentCaptor.forClass(StateHistory.class);
        verify(stateHistoryRepository).save(captor.capture());
        assertEquals("REFUND_STATUS_UNKNOWN", captor.getValue().getState());
    }

    @Test
    void processRefund_throwsRuntimeOnUnexpectedException() {
        Order order = newOrder();
        Return ret = newReturn(orderId);

        when(stateHistoryRepository.findByEntityTypeAndEntityIdAndState("Refund", ret.getId(), "REFUNDED"))
                .thenReturn(Collections.emptyList());

        when(restTemplate.postForEntity(
                eq("http://localhost:8080/mock-payment/refund"),
                any(HttpEntity.class),
                eq(RefundResponse.class)))
                .thenThrow(new RuntimeException("boom"));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> refundJobService.processRefund(ret, order, changedBy)
        );

        assertTrue(ex.getMessage().contains("Unexpected error during refund processing"));

        ArgumentCaptor<StateHistory> captor = ArgumentCaptor.forClass(StateHistory.class);
        verify(stateHistoryRepository).save(captor.capture());
        assertEquals("REFUND_STATUS_UNKNOWN", captor.getValue().getState());
    }

    // ---------------------------------------------------------------------
    // validateRefundRequest (private helper) via reflection
    // ---------------------------------------------------------------------

    private String invokeValidateRefundRequest(RefundRequest request) throws Exception {
        Method m = RefundJobService.class.getDeclaredMethod("validateRefundRequest", RefundRequest.class);
        m.setAccessible(true);
        return (String) m.invoke(refundJobService, request);
    }

    @Test
    void validateRefundRequest_returnsNullForValidRequest() throws Exception {
        RefundRequest req = new RefundRequest(
                "ret-1",
                "ord-1",
                "tx-1",
                10.0,
                "user@example.com",
                "reason"
        );

        String result = invokeValidateRefundRequest(req);
        assertNull(result);
    }

    @Test
    void validateRefundRequest_checksRequiredFieldsAndFormats() throws Exception {
        // Missing returnId
        RefundRequest req1 = new RefundRequest(
                null,
                "ord-1",
                "tx-1",
                10.0,
                "user@example.com",
                null
        );
        assertEquals("returnId is required", invokeValidateRefundRequest(req1));

        // Missing orderId
        RefundRequest req2 = new RefundRequest(
                "ret-1",
                "  ",
                "tx-1",
                10.0,
                "user@example.com",
                null
        );
        assertEquals("orderId is required", invokeValidateRefundRequest(req2));

        // Missing transactionId
        RefundRequest req3 = new RefundRequest(
                "ret-1",
                "ord-1",
                null,
                10.0,
                "user@example.com",
                null
        );
        assertEquals("transactionId is required", invokeValidateRefundRequest(req3));

        // Amount null
        RefundRequest req4 = new RefundRequest(
                "ret-1",
                "ord-1",
                "tx-1",
                null,
                "user@example.com",
                null
        );
        assertEquals("amount is required", invokeValidateRefundRequest(req4));

        // Amount <= 0
        RefundRequest req5 = new RefundRequest(
                "ret-1",
                "ord-1",
                "tx-1",
                0.0,
                "user@example.com",
                null
        );
        assertEquals("amount must be greater than zero", invokeValidateRefundRequest(req5));

        // Missing customerEmail
        RefundRequest req6 = new RefundRequest(
                "ret-1",
                "ord-1",
                "tx-1",
                10.0,
                "   ",
                null
        );
        assertEquals("customerEmail is required", invokeValidateRefundRequest(req6));

        // Invalid customerEmail format
        RefundRequest req7 = new RefundRequest(
                "ret-1",
                "ord-1",
                "tx-1",
                10.0,
                "invalid-email",
                null
        );
        assertEquals("customerEmail format is invalid", invokeValidateRefundRequest(req7));
    }
}

