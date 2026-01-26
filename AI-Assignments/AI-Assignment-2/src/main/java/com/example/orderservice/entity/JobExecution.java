package com.example.orderservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_execution")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobExecution {
    
    public enum JobType {
        INVOICE_GENERATION,
        REFUND_PROCESSING
    }
    
    public enum JobStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        RETRY
    }
    
    @Id
    @GeneratedValue
    private UUID id;

    /**
     * Optimistic locking version field to ensure that a given job is only
     * transitioned to IN_PROGRESS by a single scheduler instance, preventing
     * duplicate processing when multiple nodes poll the same pending queue.
     */
    @Version
    @Column(name = "version")
    private Long version;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false)
    private JobType jobType;
    
    @Column(name = "entity_id", nullable = false)
    private UUID entityId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private JobStatus status;
    
    @Column(name = "retry_count")
    private int retryCount = 0;
    
    @Column(name = "max_retries")
    private int maxRetries = 3;
    
    @Column(name = "last_attempt")
    private Instant lastAttempt;
    
    @Column(name = "next_attempt")
    private Instant nextAttempt;
    
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    
    @Column(name = "result", length = 1000)
    private String result;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
    
    @Column(name = "idempotency_key", length = 255, unique = true)
    private String idempotencyKey;
    
    public void incrementRetryCount() {
        this.retryCount++;
        this.updatedAt = Instant.now();
    }
    
    public boolean shouldRetry() {
        return this.status == JobStatus.RETRY && this.retryCount < this.maxRetries;
    }
    
    public void markInProgress() {
        this.status = JobStatus.IN_PROGRESS;
        this.lastAttempt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    public void markCompleted(String result) {
        this.status = JobStatus.COMPLETED;
        this.result = result;
        this.updatedAt = Instant.now();
    }
    
    public void markFailed(String errorMessage) {
        this.errorMessage = errorMessage;
        Instant now = Instant.now();
        // Derive a monotonic baseline so that nextAttempt never moves
        // backwards relative to the lastAttempt even if the system clock
        // skews slightly.
        Instant baseline = (this.lastAttempt != null && now.isBefore(this.lastAttempt))
                ? this.lastAttempt
                : now;
        this.updatedAt = baseline;

        // If we still have retries left, increment the retry counter, compute
        // the next_attempt with exponential backoff, and move the job into
        // RETRY. Otherwise, mark it as a terminal FAILED job so it will no
        // longer be picked up by the retry scheduler.
        if (this.retryCount < this.maxRetries) {
            this.retryCount++;
            // Exponential backoff: 5s, 25s, 125s for retryCount 1,2,3,...
            long delaySeconds = (long) Math.pow(5, this.retryCount);
            this.nextAttempt = baseline.plusSeconds(delaySeconds);
            this.status = JobStatus.RETRY;
        } else {
            this.status = JobStatus.FAILED;
        }
    }
}