package com.example.orderservice.service;

import com.example.orderservice.entity.JobExecution;
import com.example.orderservice.entity.JobExecution.JobStatus;
import com.example.orderservice.entity.JobExecution.JobType;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.Return;
import com.example.orderservice.dto.ReturnStatus;
import com.example.orderservice.repository.JobExecutionRepository;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.repository.ReturnRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class JobScheduler {

    private final JobExecutionRepository jobExecutionRepository;
    private final OrderRepository orderRepository;
    private final ReturnRepository returnRepository;
    private final InvoiceJobService invoiceJobService;
    private final RefundJobService refundJobService;

    @Autowired
    public JobScheduler(
            JobExecutionRepository jobExecutionRepository,
            OrderRepository orderRepository,
            ReturnRepository returnRepository,
            InvoiceJobService invoiceJobService,
            RefundJobService refundJobService) {
        this.jobExecutionRepository = jobExecutionRepository;
        this.orderRepository = orderRepository;
        this.returnRepository = returnRepository;
        this.invoiceJobService = invoiceJobService;
        this.refundJobService = refundJobService;
    }

    /**
     * Create a new job for invoice generation
     * 
     * @param orderId The order ID for which to generate an invoice
     * @return The created job
     */
    @Transactional
    @Retryable(retryFor = TransientDataAccessException.class, maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2))
    public JobExecution scheduleInvoiceJob(UUID orderId) {
        // Check if a job already exists for this order and type.
        List<JobExecution> existingJobs = jobExecutionRepository.findByEntityIdAndJobType(orderId,
                JobType.INVOICE_GENERATION);
        if (!existingJobs.isEmpty()) {
            // Reuse any non-terminal job (PENDING, IN_PROGRESS, RETRY,
            // COMPLETED) to guarantee idempotency even if idempotency keys
            // were missing or altered in older records.
            for (JobExecution job : existingJobs) {
                if (job.getStatus() != JobStatus.FAILED) {
                    return job;
                }
            }
        }

        // Generate idempotency key
        String idempotencyKey = "INVOICE-" + orderId.toString();

        // Check for existing job with same idempotency key
        Optional<JobExecution> existingJob = jobExecutionRepository.findByIdempotencyKey(idempotencyKey);
        if (existingJob.isPresent()) {
            return existingJob.get();
        }

        // Create a new job, guarding against races on the unique
        // idempotency_key constraint by falling back to the existing job if
        // another thread created it first.
        JobExecution job = new JobExecution();
        job.setJobType(JobType.INVOICE_GENERATION);
        job.setEntityId(orderId);
        job.setStatus(JobStatus.PENDING);
        job.setIdempotencyKey(idempotencyKey);

        try {
            return jobExecutionRepository.save(job);
        } catch (DataIntegrityViolationException ex) {
            // Another transaction likely inserted the same idempotency key.
            return jobExecutionRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> ex);
        }
    }

    /**
     * Create a new job for refund processing
     * 
     * @param returnId The return ID for which to process a refund
     * @param orderId  The associated order ID
     * @return The created job
     */
    @Transactional
    @Retryable(retryFor = TransientDataAccessException.class, maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2))
    public JobExecution scheduleRefundJob(UUID returnId, UUID orderId) {
        // Check if a job already exists for this return and type.
        List<JobExecution> existingJobs = jobExecutionRepository.findByEntityIdAndJobType(returnId,
                JobType.REFUND_PROCESSING);
        if (!existingJobs.isEmpty()) {
            // Reuse any non-terminal job (PENDING, IN_PROGRESS, RETRY,
            // COMPLETED) to guarantee idempotency even if idempotency keys
            // were missing or altered in older records.
            for (JobExecution job : existingJobs) {
                if (job.getStatus() != JobStatus.FAILED) {
                    return job;
                }
            }
        }

        // Generate idempotency key
        String idempotencyKey = "REFUND-" + returnId.toString();

        // Check for existing job with same idempotency key
        Optional<JobExecution> existingJob = jobExecutionRepository.findByIdempotencyKey(idempotencyKey);
        if (existingJob.isPresent()) {
            return existingJob.get();
        }

        // Create a new job with defensive handling for duplicate
        // idempotency_key values under concurrent scheduling.
        JobExecution job = new JobExecution();
        job.setJobType(JobType.REFUND_PROCESSING);
        job.setEntityId(returnId);
        job.setStatus(JobStatus.PENDING);
        job.setIdempotencyKey(idempotencyKey);

        try {
            return jobExecutionRepository.save(job);
        } catch (DataIntegrityViolationException ex) {
            return jobExecutionRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> ex);
        }
    }

    /**
     * Process pending invoice jobs
     */
    @Scheduled(fixedRate = 30000) // Run every 30 seconds
    @Transactional
    @Retryable(retryFor = TransientDataAccessException.class, maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2))
    public void processInvoiceJobs() {
        // Get pending invoice jobs
        List<JobExecution> pendingJobs = jobExecutionRepository.findPendingJobsOrderedByCreatedAt(
                JobStatus.PENDING, JobType.INVOICE_GENERATION);

        if (pendingJobs.isEmpty()) {
            log.debug("No pending invoice jobs to process");
        } else {
            log.info("Processing {} pending invoice job(s)", pendingJobs.size());
        }

        for (JobExecution job : pendingJobs) {
            try {
                // Mark job as in progress. With optimistic locking on
                // JobExecution, only one scheduler instance will succeed in
                // moving this job out of PENDING; others will hit an
                // ObjectOptimisticLockingFailureException and skip.
                job.markInProgress();
                jobExecutionRepository.save(job);

                // Get the order
                Order order = orderRepository.findById(job.getEntityId())
                        .orElseThrow(() -> new IllegalArgumentException("Order not found: " + job.getEntityId()));

                // Process the invoice synchronously so that failures are captured
                invoiceJobService.generateInvoice(order.getId(), order.getEmail());

                // Mark as completed only if invoice generation succeeded
                job.markCompleted("Invoice generation completed for order: " + order.getId());
                jobExecutionRepository.save(job);
                log.info("Invoice job {} completed for order {}", job.getId(), order.getId());
            } catch (ObjectOptimisticLockingFailureException e) {
                // Another scheduler instance has already claimed or updated this job.
                // Simply skip further processing on this node.
                continue;
            } catch (IllegalArgumentException e) {
                // Permanent failure due to bad entityId (order missing) – do
                // not retry this job again.
                String message = e.getMessage() != null ? e.getMessage() : "Order not found";
                if (message.startsWith("Order not found")) {
                    job.setRetryCount(job.getMaxRetries());
                    job.markFailed("Skipping invoice job: " + message);
                } else {
                    job.markFailed(message);
                }
                jobExecutionRepository.save(job);
                log.warn("Invoice job {} failed permanently: {}", job.getId(), message);
            } catch (Exception e) {
                // Mark job as failed (with retry/backoff where applicable)
                job.markFailed(e.getMessage());
                jobExecutionRepository.save(job);
                log.error("Invoice job {} failed with unexpected error: {}", job.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Process pending refund jobs
     */
    @Scheduled(fixedRate = 30000) // Run every 30 seconds
    @Transactional
    @Retryable(retryFor = TransientDataAccessException.class, maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2))
    public void processRefundJobs() {
        // Get pending refund jobs
        List<JobExecution> pendingJobs = jobExecutionRepository.findPendingJobsOrderedByCreatedAt(
                JobStatus.PENDING, JobType.REFUND_PROCESSING);

        if (pendingJobs.isEmpty()) {
            log.debug("No pending refund jobs to process");
        } else {
            log.info("Processing {} pending refund job(s)", pendingJobs.size());
        }

        for (JobExecution job : pendingJobs) {
            try {
                // Mark job as in progress. Optimistic locking ensures only one
                // scheduler instance can claim a given job.
                job.markInProgress();
                jobExecutionRepository.save(job);

                // Get the return
                Return returnObj = returnRepository.findById(job.getEntityId())
                        .orElseThrow(() -> new IllegalArgumentException("Return not found: " + job.getEntityId()));

                // If the return is no longer COMPLETED (e.g., manually
                // adjusted or rolled back), skip refund processing to avoid
                // issuing a refund for an ineligible or changed return.
                if (returnObj.getStatus() != ReturnStatus.COMPLETED) {
                    job.markFailed("Skipping refund: return is in status " + returnObj.getStatus());
                    jobExecutionRepository.save(job);
                    log.info("Skipping refund job {} for return {} due to status {}", job.getId(),
                            returnObj.getId(), returnObj.getStatus());
                    continue;
                }

                // Get the order
                Order order = orderRepository.findById(returnObj.getOrderId())
                        .orElseThrow(() -> new IllegalArgumentException("Order not found: " + returnObj.getOrderId()));

                // Default changed by - system user
                UUID systemUserId = UUID.fromString("00000000-0000-0000-0000-000000000000");

                // Process the refund synchronously so that failures are captured
                refundJobService.processRefund(returnObj, order, systemUserId);

                // Mark as completed only if refund processing succeeded
                job.markCompleted("Refund processing completed for return: " + returnObj.getId());
                jobExecutionRepository.save(job);
                log.info("Refund job {} completed for return {} and order {}", job.getId(), returnObj.getId(),
                        order.getId());
            } catch (ObjectOptimisticLockingFailureException e) {
                // Job already claimed/updated by another node; skip.
                continue;
            } catch (IllegalArgumentException e) {
                // Permanent failure due to missing return/order – do not
                // schedule retries.
                String message = e.getMessage() != null ? e.getMessage() : "";
                if (message.startsWith("Return not found") || message.startsWith("Order not found")) {
                    job.setRetryCount(job.getMaxRetries());
                    job.markFailed("Skipping refund job: " + message);
                } else {
                    job.markFailed(message);
                }
                jobExecutionRepository.save(job);
                log.warn("Refund job {} failed permanently: {}", job.getId(), message);
            } catch (Exception e) {
                // Mark job as failed (with retry/backoff where applicable)
                job.markFailed(e.getMessage());
                jobExecutionRepository.save(job);
                log.error("Refund job {} failed with unexpected error: {}", job.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Process jobs that need to be retried
     */
    @Scheduled(fixedRate = 60000) // Run every minute
    @Transactional
    @Retryable(retryFor = TransientDataAccessException.class, maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2))
    public void processRetryJobs() {
        // Get jobs that need to be retried
        List<JobExecution> retryJobs = jobExecutionRepository.findJobsForRetry(JobStatus.RETRY, Instant.now());

        if (retryJobs.isEmpty()) {
            log.debug("No jobs eligible for retry");
        } else {
            log.info("Processing {} retry job(s)", retryJobs.size());
        }

        for (JobExecution job : retryJobs) {
            try {
                // Move the job back to IN_PROGRESS; retry counters and
                // next_attempt are managed centrally in JobExecution.markFailed.
                job.markInProgress();
                jobExecutionRepository.save(job);

                // Default changed by - system user
                UUID systemUserId = UUID.fromString("00000000-0000-0000-0000-000000000000");

                // Process based on job type
                if (job.getJobType() == JobType.INVOICE_GENERATION) {
                    Order order = orderRepository.findById(job.getEntityId())
                            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + job.getEntityId()));
                    invoiceJobService.generateInvoice(order.getId(), order.getEmail());
                    job.markCompleted("Invoice generation retry completed for order: " + order.getId());
                } else if (job.getJobType() == JobType.REFUND_PROCESSING) {
                    Return returnObj = returnRepository.findById(job.getEntityId())
                            .orElseThrow(() -> new IllegalArgumentException("Return not found: " + job.getEntityId()));

                    // Skip refunds for returns that are no longer COMPLETED.
                    if (returnObj.getStatus() != ReturnStatus.COMPLETED) {
                        job.markFailed("Skipping refund retry: return is in status " + returnObj.getStatus());
                    } else {
                        Order order = orderRepository.findById(returnObj.getOrderId())
                                .orElseThrow(() -> new IllegalArgumentException(
                                        "Order not found: " + returnObj.getOrderId()));
                        refundJobService.processRefund(returnObj, order, systemUserId);
                        job.markCompleted("Refund processing retry completed for return: " + returnObj.getId());
                    }
                }

                jobExecutionRepository.save(job);
                log.info("Retry job {} of type {} completed with status {}", job.getId(), job.getJobType(),
                        job.getStatus());
            } catch (ObjectOptimisticLockingFailureException e) {
                // Another scheduler already advanced this job; ignore.
                continue;
            } catch (IllegalArgumentException e) {
                // Permanent failure due to missing entities during retry.
                String message = e.getMessage() != null ? e.getMessage() : "";
                if (message.startsWith("Order not found") || message.startsWith("Return not found")) {
                    job.setRetryCount(job.getMaxRetries());
                    job.markFailed("Skipping retry job: " + message);
                } else {
                    job.markFailed(message);
                }
                jobExecutionRepository.save(job);
                log.warn("Retry job {} failed permanently: {}", job.getId(), message);
            } catch (Exception e) {
                // Mark job as failed (with retry/backoff)
                job.markFailed(e.getMessage());
                jobExecutionRepository.save(job);
                log.error("Retry job {} failed with unexpected error: {}", job.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Update job status to completed
     * 
     * @param jobId  The job ID
     * @param result The job result
     */
    @Transactional
    @Retryable(retryFor = TransientDataAccessException.class, maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2))
    public void completeJob(UUID jobId, String result) {
        JobExecution job = jobExecutionRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        job.markCompleted(result);
        jobExecutionRepository.save(job);
    }

    /**
     * Update job status to failed
     * 
     * @param jobId        The job ID
     * @param errorMessage The error message
     */
    @Transactional
    @Retryable(retryFor = TransientDataAccessException.class, maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2))
    public void failJob(UUID jobId, String errorMessage) {
        JobExecution job = jobExecutionRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        job.markFailed(errorMessage);
        jobExecutionRepository.save(job);
    }
}