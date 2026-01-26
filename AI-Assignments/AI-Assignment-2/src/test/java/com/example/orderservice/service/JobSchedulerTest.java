package com.example.orderservice.service;

import com.example.orderservice.dto.ReturnStatus;
import com.example.orderservice.entity.JobExecution;
import com.example.orderservice.entity.JobExecution.JobStatus;
import com.example.orderservice.entity.JobExecution.JobType;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.Return;
import com.example.orderservice.repository.JobExecutionRepository;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.repository.ReturnRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JobSchedulerTest {

    @Mock
    private JobExecutionRepository jobExecutionRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ReturnRepository returnRepository;

    @Mock
    private InvoiceJobService invoiceJobService;

    @Mock
    private RefundJobService refundJobService;

    @InjectMocks
    private JobScheduler jobScheduler;

    private UUID orderId;
    private UUID returnId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        orderId = UUID.randomUUID();
        returnId = UUID.randomUUID();
    }

    private JobExecution newJob(JobType type, UUID entityId, JobStatus status) {
        JobExecution job = new JobExecution();
        job.setId(UUID.randomUUID());
        job.setJobType(type);
        job.setEntityId(entityId);
        job.setStatus(status);
        job.setMaxRetries(3);
        job.setRetryCount(0);
        job.setCreatedAt(Instant.now());
        job.setUpdatedAt(Instant.now());
        return job;
    }

    private Order newOrder(UUID id) {
        Order order = new Order();
        order.setId(id);
        order.setTransactionId(UUID.randomUUID());
        order.setCustomerId(UUID.randomUUID());
        order.setShippingAddress("123 Test Street");
        order.setEmail("customer@example.com");
        order.setProductId(UUID.randomUUID());
        order.setQuantity(1);
        order.setPrice(10.0);
        return order;
    }

    private Return newReturn(UUID id, UUID orderId, ReturnStatus status) {
        Return r = new Return();
        r.setId(id);
        r.setOrderId(orderId);
        r.setStatus(status);
        return r;
    }

    // ---------------------------------------------------------------------
    // scheduleInvoiceJob
    // ---------------------------------------------------------------------

    @Test
    void scheduleInvoiceJob_reusesExistingNonFailedJob() {
        JobExecution existing = newJob(JobType.INVOICE_GENERATION, orderId, JobStatus.PENDING);
        when(jobExecutionRepository.findByEntityIdAndJobType(orderId, JobType.INVOICE_GENERATION))
                .thenReturn(Collections.singletonList(existing));

        JobExecution result = jobScheduler.scheduleInvoiceJob(orderId);

        assertEquals(existing, result);
        verify(jobExecutionRepository).findByEntityIdAndJobType(orderId, JobType.INVOICE_GENERATION);
        verifyNoInteractions(orderRepository, returnRepository, invoiceJobService, refundJobService);
    }

    @Test
    void scheduleInvoiceJob_skipsFailedJobsAndCreatesNewWhenNoExistingIdempotent() {
        JobExecution failed = newJob(JobType.INVOICE_GENERATION, orderId, JobStatus.FAILED);
        when(jobExecutionRepository.findByEntityIdAndJobType(orderId, JobType.INVOICE_GENERATION))
                .thenReturn(Collections.singletonList(failed));
        when(jobExecutionRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());

        JobExecution saved = newJob(JobType.INVOICE_GENERATION, orderId, JobStatus.PENDING);
        when(jobExecutionRepository.save(any(JobExecution.class))).thenReturn(saved);

        JobExecution result = jobScheduler.scheduleInvoiceJob(orderId);

        assertEquals(saved, result);
        verify(jobExecutionRepository).save(any(JobExecution.class));
    }

    @Test
    void scheduleInvoiceJob_returnsExistingByIdempotencyKeyOnConstraintViolation() {
        when(jobExecutionRepository.findByEntityIdAndJobType(orderId, JobType.INVOICE_GENERATION))
                .thenReturn(Collections.emptyList());

        String key = "INVOICE-" + orderId;
        when(jobExecutionRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(newJob(
                JobType.INVOICE_GENERATION, orderId, JobStatus.PENDING)));
        when(jobExecutionRepository.save(any(JobExecution.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        JobExecution result = jobScheduler.scheduleInvoiceJob(orderId);

        assertNotNull(result);
        assertEquals(JobType.INVOICE_GENERATION, result.getJobType());
        verify(jobExecutionRepository, times(1)).findByIdempotencyKey(key);
    }

    // ---------------------------------------------------------------------
    // scheduleRefundJob
    // ---------------------------------------------------------------------

    @Test
    void scheduleRefundJob_reusesExistingNonFailedJob() {
        JobExecution existing = newJob(JobType.REFUND_PROCESSING, returnId, JobStatus.IN_PROGRESS);
        when(jobExecutionRepository.findByEntityIdAndJobType(returnId, JobType.REFUND_PROCESSING))
                .thenReturn(Collections.singletonList(existing));

        JobExecution result = jobScheduler.scheduleRefundJob(returnId, orderId);

        assertEquals(existing, result);
    }

    @Test
    void scheduleRefundJob_skipsFailedAndCreatesNewWhenNoExistingIdempotent() {
        JobExecution failed = newJob(JobType.REFUND_PROCESSING, returnId, JobStatus.FAILED);
        when(jobExecutionRepository.findByEntityIdAndJobType(returnId, JobType.REFUND_PROCESSING))
                .thenReturn(Collections.singletonList(failed));
        when(jobExecutionRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());

        JobExecution saved = newJob(JobType.REFUND_PROCESSING, returnId, JobStatus.PENDING);
        when(jobExecutionRepository.save(any(JobExecution.class))).thenReturn(saved);

        JobExecution result = jobScheduler.scheduleRefundJob(returnId, orderId);

        assertEquals(saved, result);
        verify(jobExecutionRepository).save(any(JobExecution.class));
    }

    @Test
    void scheduleRefundJob_returnsExistingByIdempotencyKeyOnConstraintViolation() {
        when(jobExecutionRepository.findByEntityIdAndJobType(returnId, JobType.REFUND_PROCESSING))
                .thenReturn(Collections.emptyList());

        String key = "REFUND-" + returnId;
        when(jobExecutionRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(newJob(
                JobType.REFUND_PROCESSING, returnId, JobStatus.PENDING)));
        when(jobExecutionRepository.save(any(JobExecution.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        JobExecution result = jobScheduler.scheduleRefundJob(returnId, orderId);

        assertNotNull(result);
        assertEquals(JobType.REFUND_PROCESSING, result.getJobType());
        verify(jobExecutionRepository, times(1)).findByIdempotencyKey(key);
    }

    // ---------------------------------------------------------------------
    // processInvoiceJobs
    // ---------------------------------------------------------------------

    @Test
    void processInvoiceJobs_whenNoPendingJobs_logsAndDoesNothing() {
        when(jobExecutionRepository.findPendingJobsOrderedByCreatedAt(JobStatus.PENDING, JobType.INVOICE_GENERATION))
                .thenReturn(Collections.emptyList());

        jobScheduler.processInvoiceJobs();

        verify(jobExecutionRepository).findPendingJobsOrderedByCreatedAt(JobStatus.PENDING,
                JobType.INVOICE_GENERATION);
        verifyNoInteractions(orderRepository, invoiceJobService, returnRepository, refundJobService);
    }

    @Test
    void processInvoiceJobs_happyPathCompletesJob() {
        JobExecution job = newJob(JobType.INVOICE_GENERATION, orderId, JobStatus.PENDING);
        List<JobExecution> jobs = new ArrayList<>();
        jobs.add(job);
        when(jobExecutionRepository.findPendingJobsOrderedByCreatedAt(JobStatus.PENDING, JobType.INVOICE_GENERATION))
                .thenReturn(jobs);

        Order order = newOrder(orderId);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        doNothing().when(invoiceJobService).generateInvoice(order.getId(), order.getEmail());
        when(jobExecutionRepository.save(any(JobExecution.class))).thenReturn(job);

        jobScheduler.processInvoiceJobs();

        verify(invoiceJobService).generateInvoice(order.getId(), order.getEmail());
        verify(jobExecutionRepository, times(2)).save(any(JobExecution.class));
        assertEquals(JobStatus.COMPLETED, job.getStatus());
    }

    @Test
    void processInvoiceJobs_skipsWhenOptimisticLockingFailure() {
        JobExecution job = newJob(JobType.INVOICE_GENERATION, orderId, JobStatus.PENDING);
        when(jobExecutionRepository.findPendingJobsOrderedByCreatedAt(JobStatus.PENDING, JobType.INVOICE_GENERATION))
                .thenReturn(Collections.singletonList(job));
        when(jobExecutionRepository.save(any(JobExecution.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(JobExecution.class, job.getId()));

        jobScheduler.processInvoiceJobs();

        verify(jobExecutionRepository).save(any(JobExecution.class));
        verifyNoInteractions(orderRepository, invoiceJobService);
    }

    @Test
    void processInvoiceJobs_marksFailedPermanentlyWhenOrderMissing() {
        JobExecution job = newJob(JobType.INVOICE_GENERATION, orderId, JobStatus.PENDING);
        when(jobExecutionRepository.findPendingJobsOrderedByCreatedAt(JobStatus.PENDING, JobType.INVOICE_GENERATION))
                .thenReturn(Collections.singletonList(job));
        when(jobExecutionRepository.save(any(JobExecution.class))).thenReturn(job);
        when(orderRepository.findById(orderId))
                .thenThrow(new IllegalArgumentException("Order not found: " + orderId));

        jobScheduler.processInvoiceJobs();

        assertEquals(JobStatus.FAILED, job.getStatus());
        assertEquals(job.getMaxRetries(), job.getRetryCount());
        verify(jobExecutionRepository, times(2)).save(any(JobExecution.class));
    }

    @Test
    void processInvoiceJobs_marksFailedOnUnexpectedError() {
        JobExecution job = newJob(JobType.INVOICE_GENERATION, orderId, JobStatus.PENDING);
        when(jobExecutionRepository.findPendingJobsOrderedByCreatedAt(JobStatus.PENDING, JobType.INVOICE_GENERATION))
                .thenReturn(Collections.singletonList(job));
        when(jobExecutionRepository.save(any(JobExecution.class))).thenReturn(job);

        Order order = newOrder(orderId);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        doThrow(new RuntimeException("Unexpected failure"))
                .when(invoiceJobService)
                .generateInvoice(order.getId(), order.getEmail());

        jobScheduler.processInvoiceJobs();

        assertEquals(JobStatus.RETRY, job.getStatus());
        verify(jobExecutionRepository, times(2)).save(any(JobExecution.class));
    }

    // ---------------------------------------------------------------------
    // processRefundJobs
    // ---------------------------------------------------------------------

    @Test
    void processRefundJobs_whenNoPendingJobs_logsAndDoesNothing() {
        when(jobExecutionRepository.findPendingJobsOrderedByCreatedAt(JobStatus.PENDING, JobType.REFUND_PROCESSING))
                .thenReturn(Collections.emptyList());

        jobScheduler.processRefundJobs();

        verify(jobExecutionRepository).findPendingJobsOrderedByCreatedAt(JobStatus.PENDING,
                JobType.REFUND_PROCESSING);
        verifyNoInteractions(orderRepository, refundJobService);
    }

    @Test
    void processRefundJobs_happyPathCompletesJob() {
        JobExecution job = newJob(JobType.REFUND_PROCESSING, returnId, JobStatus.PENDING);
        when(jobExecutionRepository.findPendingJobsOrderedByCreatedAt(JobStatus.PENDING, JobType.REFUND_PROCESSING))
                .thenReturn(Collections.singletonList(job));

        Return returnObj = newReturn(returnId, orderId, ReturnStatus.COMPLETED);
        when(returnRepository.findById(returnId)).thenReturn(Optional.of(returnObj));

        Order order = newOrder(orderId);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        doNothing().when(refundJobService).processRefund(any(Return.class), any(Order.class), any(UUID.class));
        when(jobExecutionRepository.save(any(JobExecution.class))).thenReturn(job);

        jobScheduler.processRefundJobs();

        verify(refundJobService).processRefund(any(Return.class), any(Order.class), any(UUID.class));
        assertEquals(JobStatus.COMPLETED, job.getStatus());
        verify(jobExecutionRepository, times(2)).save(any(JobExecution.class));
    }

    @Test
    void processRefundJobs_skipsWhenOptimisticLockingFailure() {
        JobExecution job = newJob(JobType.REFUND_PROCESSING, returnId, JobStatus.PENDING);
        when(jobExecutionRepository.findPendingJobsOrderedByCreatedAt(JobStatus.PENDING, JobType.REFUND_PROCESSING))
                .thenReturn(Collections.singletonList(job));
        when(jobExecutionRepository.save(any(JobExecution.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(JobExecution.class, job.getId()));

        jobScheduler.processRefundJobs();

        verify(jobExecutionRepository).save(any(JobExecution.class));
        verifyNoInteractions(returnRepository, refundJobService);
    }

    @Test
    void processRefundJobs_skipsWhenReturnNotCompleted() {
        JobExecution job = newJob(JobType.REFUND_PROCESSING, returnId, JobStatus.PENDING);
        when(jobExecutionRepository.findPendingJobsOrderedByCreatedAt(JobStatus.PENDING, JobType.REFUND_PROCESSING))
                .thenReturn(Collections.singletonList(job));
        when(jobExecutionRepository.save(any(JobExecution.class))).thenReturn(job);

        Return returnObj = newReturn(returnId, orderId, ReturnStatus.REQUESTED);
        when(returnRepository.findById(returnId)).thenReturn(Optional.of(returnObj));

        jobScheduler.processRefundJobs();

        assertEquals(JobStatus.RETRY, job.getStatus());
        verify(jobExecutionRepository, times(2)).save(any(JobExecution.class));
        verifyNoInteractions(orderRepository, refundJobService);
    }

    @Test
    void processRefundJobs_marksFailedWhenReturnMissing() {
        JobExecution job = newJob(JobType.REFUND_PROCESSING, returnId, JobStatus.PENDING);
        when(jobExecutionRepository.findPendingJobsOrderedByCreatedAt(JobStatus.PENDING, JobType.REFUND_PROCESSING))
                .thenReturn(Collections.singletonList(job));
        when(jobExecutionRepository.save(any(JobExecution.class))).thenReturn(job);
        when(returnRepository.findById(returnId))
                .thenThrow(new IllegalArgumentException("Return not found: " + returnId));

        jobScheduler.processRefundJobs();

        assertEquals(JobStatus.FAILED, job.getStatus());
        assertEquals(job.getMaxRetries(), job.getRetryCount());
        verify(jobExecutionRepository, times(2)).save(any(JobExecution.class));
    }

    @Test
    void processRefundJobs_marksFailedWhenOrderMissing() {
        JobExecution job = newJob(JobType.REFUND_PROCESSING, returnId, JobStatus.PENDING);
        when(jobExecutionRepository.findPendingJobsOrderedByCreatedAt(JobStatus.PENDING, JobType.REFUND_PROCESSING))
                .thenReturn(Collections.singletonList(job));
        when(jobExecutionRepository.save(any(JobExecution.class))).thenReturn(job);

        Return returnObj = newReturn(returnId, orderId, ReturnStatus.COMPLETED);
        when(returnRepository.findById(returnId)).thenReturn(Optional.of(returnObj));
        when(orderRepository.findById(orderId))
                .thenThrow(new IllegalArgumentException("Order not found: " + orderId));

        jobScheduler.processRefundJobs();

        assertEquals(JobStatus.FAILED, job.getStatus());
        assertEquals(job.getMaxRetries(), job.getRetryCount());
        verify(jobExecutionRepository, times(2)).save(any(JobExecution.class));
    }

    @Test
    void processRefundJobs_marksFailedOnUnexpectedError() {
        JobExecution job = newJob(JobType.REFUND_PROCESSING, returnId, JobStatus.PENDING);
        when(jobExecutionRepository.findPendingJobsOrderedByCreatedAt(JobStatus.PENDING, JobType.REFUND_PROCESSING))
                .thenReturn(Collections.singletonList(job));
        when(jobExecutionRepository.save(any(JobExecution.class))).thenReturn(job);

        Return returnObj = newReturn(returnId, orderId, ReturnStatus.COMPLETED);
        when(returnRepository.findById(returnId)).thenReturn(Optional.of(returnObj));

        Order order = newOrder(orderId);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        doThrow(new RuntimeException("Unexpected failure"))
                .when(refundJobService)
                .processRefund(any(Return.class), any(Order.class), any(UUID.class));

        jobScheduler.processRefundJobs();

        assertEquals(JobStatus.RETRY, job.getStatus());
        verify(jobExecutionRepository, times(2)).save(any(JobExecution.class));
    }

    // ---------------------------------------------------------------------
    // processRetryJobs
    // ---------------------------------------------------------------------

    @Test
    void processRetryJobs_whenNoJobs_logsAndDoesNothing() {
        when(jobExecutionRepository.findJobsForRetry(any(), any(Instant.class)))
                .thenReturn(Collections.emptyList());

        jobScheduler.processRetryJobs();

        verify(jobExecutionRepository).findJobsForRetry(any(JobStatus.class), any(Instant.class));
        verifyNoInteractions(orderRepository, returnRepository, invoiceJobService, refundJobService);
    }

    @Test
    void processRetryJobs_retriesInvoiceJobSuccessfully() {
        JobExecution job = newJob(JobType.INVOICE_GENERATION, orderId, JobStatus.RETRY);
        when(jobExecutionRepository.findJobsForRetry(any(), any(Instant.class)))
                .thenReturn(Collections.singletonList(job));
        when(jobExecutionRepository.save(any(JobExecution.class))).thenReturn(job);

        Order order = newOrder(orderId);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        doNothing().when(invoiceJobService).generateInvoice(order.getId(), order.getEmail());

        jobScheduler.processRetryJobs();

        assertEquals(JobStatus.COMPLETED, job.getStatus());
        verify(invoiceJobService).generateInvoice(order.getId(), order.getEmail());
        verify(jobExecutionRepository, times(2)).save(any(JobExecution.class));
    }

    @Test
    void processRetryJobs_retriesRefundJobSuccessfully() {
        JobExecution job = newJob(JobType.REFUND_PROCESSING, returnId, JobStatus.RETRY);
        when(jobExecutionRepository.findJobsForRetry(any(), any(Instant.class)))
                .thenReturn(Collections.singletonList(job));
        when(jobExecutionRepository.save(any(JobExecution.class))).thenReturn(job);

        Return returnObj = newReturn(returnId, orderId, ReturnStatus.COMPLETED);
        when(returnRepository.findById(returnId)).thenReturn(Optional.of(returnObj));

        Order order = newOrder(orderId);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        doNothing().when(refundJobService).processRefund(any(Return.class), any(Order.class), any(UUID.class));

        jobScheduler.processRetryJobs();

        assertEquals(JobStatus.COMPLETED, job.getStatus());
        verify(refundJobService).processRefund(any(Return.class), any(Order.class), any(UUID.class));
        verify(jobExecutionRepository, times(2)).save(any(JobExecution.class));
    }

    @Test
    void processRetryJobs_skipsRefundRetryWhenReturnNotCompleted() {
        JobExecution job = newJob(JobType.REFUND_PROCESSING, returnId, JobStatus.RETRY);
        when(jobExecutionRepository.findJobsForRetry(any(), any(Instant.class)))
                .thenReturn(Collections.singletonList(job));
        when(jobExecutionRepository.save(any(JobExecution.class))).thenReturn(job);

        Return returnObj = newReturn(returnId, orderId, ReturnStatus.REQUESTED);
        when(returnRepository.findById(returnId)).thenReturn(Optional.of(returnObj));

        jobScheduler.processRetryJobs();

        // markFailed keeps job in RETRY while retries remain
        assertEquals(JobStatus.RETRY, job.getStatus());
        verify(jobExecutionRepository, times(2)).save(any(JobExecution.class));
        verifyNoInteractions(refundJobService, orderRepository);
    }

    @Test
    void processRetryJobs_marksFailedWhenEntitiesMissing() {
        JobExecution job = newJob(JobType.INVOICE_GENERATION, orderId, JobStatus.RETRY);
        when(jobExecutionRepository.findJobsForRetry(any(), any(Instant.class)))
                .thenReturn(Collections.singletonList(job));
        when(jobExecutionRepository.save(any(JobExecution.class))).thenReturn(job);
        when(orderRepository.findById(orderId))
                .thenThrow(new IllegalArgumentException("Order not found: " + orderId));

        jobScheduler.processRetryJobs();

        assertEquals(JobStatus.FAILED, job.getStatus());
        assertEquals(job.getMaxRetries(), job.getRetryCount());
        verify(jobExecutionRepository, times(2)).save(any(JobExecution.class));
    }

    @Test
    void processRetryJobs_marksFailedOnUnexpectedError() {
        JobExecution job = newJob(JobType.INVOICE_GENERATION, orderId, JobStatus.RETRY);
        when(jobExecutionRepository.findJobsForRetry(any(), any(Instant.class)))
                .thenReturn(Collections.singletonList(job));
        when(jobExecutionRepository.save(any(JobExecution.class))).thenReturn(job);

        Order order = newOrder(orderId);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        doThrow(new RuntimeException("Unexpected failure"))
                .when(invoiceJobService)
                .generateInvoice(order.getId(), order.getEmail());

        jobScheduler.processRetryJobs();

        assertEquals(JobStatus.RETRY, job.getStatus());
        verify(jobExecutionRepository, times(2)).save(any(JobExecution.class));
    }

    @Test
    void processRetryJobs_skipsOnOptimisticLockingFailure() {
        JobExecution job = newJob(JobType.INVOICE_GENERATION, orderId, JobStatus.RETRY);
        when(jobExecutionRepository.findJobsForRetry(any(), any(Instant.class)))
                .thenReturn(Collections.singletonList(job));
        when(jobExecutionRepository.save(any(JobExecution.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(JobExecution.class, job.getId()));

        jobScheduler.processRetryJobs();

        verify(jobExecutionRepository).save(any(JobExecution.class));
        verifyNoInteractions(orderRepository, returnRepository, invoiceJobService, refundJobService);
    }

    // ---------------------------------------------------------------------
    // completeJob / failJob
    // ---------------------------------------------------------------------

    @Test
    void completeJob_happyPath() {
        JobExecution job = newJob(JobType.INVOICE_GENERATION, orderId, JobStatus.PENDING);
        UUID jobId = UUID.randomUUID();
        job.setId(jobId);
        when(jobExecutionRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobExecutionRepository.save(job)).thenReturn(job);

        jobScheduler.completeJob(jobId, "OK");

        assertEquals(JobStatus.COMPLETED, job.getStatus());
        assertEquals("OK", job.getResult());
        verify(jobExecutionRepository).save(job);
    }

    @Test
    void completeJob_throwsWhenJobMissing() {
        UUID jobId = UUID.randomUUID();
        when(jobExecutionRepository.findById(jobId)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> jobScheduler.completeJob(jobId, "OK")
        );
        assertEquals("Job not found: " + jobId, ex.getMessage());
    }

    @Test
    void failJob_happyPath() {
        JobExecution job = newJob(JobType.INVOICE_GENERATION, orderId, JobStatus.PENDING);
        UUID jobId = UUID.randomUUID();
        job.setId(jobId);
        when(jobExecutionRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobExecutionRepository.save(job)).thenReturn(job);

        jobScheduler.failJob(jobId, "error");

        // markFailed may move status to RETRY or FAILED depending on retries.
        assertNotNull(job.getErrorMessage());
        verify(jobExecutionRepository).save(job);
    }

    @Test
    void failJob_throwsWhenJobMissing() {
        UUID jobId = UUID.randomUUID();
        when(jobExecutionRepository.findById(jobId)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> jobScheduler.failJob(jobId, "error")
        );
        assertEquals("Job not found: " + jobId, ex.getMessage());
    }
}

