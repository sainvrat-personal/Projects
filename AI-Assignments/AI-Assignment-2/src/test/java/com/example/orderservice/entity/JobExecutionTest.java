package com.example.orderservice.entity;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JobExecutionTest {

    @Test
    void noArgsConstructorAndSetters_populateFieldsCorrectly() {
        JobExecution job = new JobExecution();

        UUID id = UUID.randomUUID();
        Long version = 1L;
        JobExecution.JobType jobType = JobExecution.JobType.INVOICE_GENERATION;
        UUID entityId = UUID.randomUUID();
        JobExecution.JobStatus status = JobExecution.JobStatus.PENDING;
        int retryCount = 2;
        int maxRetries = 5;
        Instant lastAttempt = Instant.now().minusSeconds(60);
        Instant nextAttempt = Instant.now().plusSeconds(60);
        String errorMessage = "Some error";
        String result = "Some result";
        Instant createdAt = Instant.now().minusSeconds(120);
        Instant updatedAt = Instant.now().minusSeconds(30);
        String idempotencyKey = "idem-key";

        job.setId(id);
        job.setVersion(version);
        job.setJobType(jobType);
        job.setEntityId(entityId);
        job.setStatus(status);
        job.setRetryCount(retryCount);
        job.setMaxRetries(maxRetries);
        job.setLastAttempt(lastAttempt);
        job.setNextAttempt(nextAttempt);
        job.setErrorMessage(errorMessage);
        job.setResult(result);
        job.setCreatedAt(createdAt);
        job.setUpdatedAt(updatedAt);
        job.setIdempotencyKey(idempotencyKey);

        assertEquals(id, job.getId());
        assertEquals(version, job.getVersion());
        assertEquals(jobType, job.getJobType());
        assertEquals(entityId, job.getEntityId());
        assertEquals(status, job.getStatus());
        assertEquals(retryCount, job.getRetryCount());
        assertEquals(maxRetries, job.getMaxRetries());
        assertEquals(lastAttempt, job.getLastAttempt());
        assertEquals(nextAttempt, job.getNextAttempt());
        assertEquals(errorMessage, job.getErrorMessage());
        assertEquals(result, job.getResult());
        assertEquals(createdAt, job.getCreatedAt());
        assertEquals(updatedAt, job.getUpdatedAt());
        assertEquals(idempotencyKey, job.getIdempotencyKey());
    }

    @Test
    void allArgsConstructor_populatesAllFields() {
        UUID id = UUID.randomUUID();
        Long version = 2L;
        JobExecution.JobType jobType = JobExecution.JobType.REFUND_PROCESSING;
        UUID entityId = UUID.randomUUID();
        JobExecution.JobStatus status = JobExecution.JobStatus.IN_PROGRESS;
        int retryCount = 1;
        int maxRetries = 4;
        Instant lastAttempt = Instant.now().minusSeconds(10);
        Instant nextAttempt = Instant.now().plusSeconds(20);
        String errorMessage = "Error";
        String result = "Result";
        Instant createdAt = Instant.now().minusSeconds(300);
        Instant updatedAt = Instant.now().minusSeconds(100);
        String idempotencyKey = "another-idem";

        JobExecution job = new JobExecution(
                id,
                version,
                jobType,
                entityId,
                status,
                retryCount,
                maxRetries,
                lastAttempt,
                nextAttempt,
                errorMessage,
                result,
                createdAt,
                updatedAt,
                idempotencyKey
        );

        assertEquals(id, job.getId());
        assertEquals(version, job.getVersion());
        assertEquals(jobType, job.getJobType());
        assertEquals(entityId, job.getEntityId());
        assertEquals(status, job.getStatus());
        assertEquals(retryCount, job.getRetryCount());
        assertEquals(maxRetries, job.getMaxRetries());
        assertEquals(lastAttempt, job.getLastAttempt());
        assertEquals(nextAttempt, job.getNextAttempt());
        assertEquals(errorMessage, job.getErrorMessage());
        assertEquals(result, job.getResult());
        assertEquals(createdAt, job.getCreatedAt());
        assertEquals(updatedAt, job.getUpdatedAt());
        assertEquals(idempotencyKey, job.getIdempotencyKey());
    }

    @Test
    void defaultFieldValues_areInitialized() {
        JobExecution job = new JobExecution();

        assertEquals(0, job.getRetryCount());
        assertEquals(3, job.getMaxRetries());
        assertNotNull(job.getCreatedAt());
        assertNotNull(job.getUpdatedAt());
        assertNull(job.getId());
        assertNull(job.getVersion());
        assertNull(job.getJobType());
        assertNull(job.getEntityId());
        assertNull(job.getStatus());
        assertNull(job.getLastAttempt());
        assertNull(job.getNextAttempt());
        assertNull(job.getErrorMessage());
        assertNull(job.getResult());
        assertNull(job.getIdempotencyKey());
    }

    @Test
    void equalsAndHashCode_coverPositiveAndNegativeCases() {
        UUID id = UUID.randomUUID();
        UUID entityId = UUID.randomUUID();
        Instant createdAt = Instant.now().minusSeconds(60);
        Instant updatedAt = Instant.now().minusSeconds(30);

        JobExecution j1 = new JobExecution(
                id,
                1L,
                JobExecution.JobType.INVOICE_GENERATION,
                entityId,
                JobExecution.JobStatus.PENDING,
                0,
                3,
                null,
                null,
                null,
                null,
                createdAt,
                updatedAt,
                "idem"
        );

        JobExecution j2 = new JobExecution(
                id,
                1L,
                JobExecution.JobType.INVOICE_GENERATION,
                entityId,
                JobExecution.JobStatus.PENDING,
                0,
                3,
                null,
                null,
                null,
                null,
                createdAt,
                updatedAt,
                "idem"
        );

        JobExecution jDifferent = new JobExecution(
                id,
                2L,
                JobExecution.JobType.REFUND_PROCESSING,
                entityId,
                JobExecution.JobStatus.COMPLETED,
                1,
                5,
                null,
                null,
                "err",
                "res",
                createdAt,
                updatedAt,
                "other-idem"
        );

        // same instance
        assertEquals(j1, j1);

        // equal values
        assertEquals(j1, j2);
        assertEquals(j1.hashCode(), j2.hashCode());

        // different content
        assertNotEquals(j1, jDifferent);

        // null and different type
        assertNotEquals(j1, null);
        assertNotEquals(j1, new Object());
    }

    @Test
    void toString_containsKeyInformationAndDoesNotThrow() {
        JobExecution job = new JobExecution();
        UUID entityId = UUID.randomUUID();
        job.setJobType(JobExecution.JobType.INVOICE_GENERATION);
        job.setEntityId(entityId);
        job.setStatus(JobExecution.JobStatus.PENDING);

        String s = job.toString();
        assertNotNull(s);
        assertTrue(s.contains("INVOICE_GENERATION"));
        assertTrue(s.contains(JobExecution.JobStatus.PENDING.name()));
        assertTrue(s.contains(entityId.toString()));
    }

    @Test
    void incrementRetryCount_increasesRetryAndUpdatesTimestamp() {
        JobExecution job = new JobExecution();
        job.setUpdatedAt(Instant.now().minusSeconds(60));
        job.setRetryCount(1);

        Instant before = Instant.now();
        job.incrementRetryCount();

        assertEquals(2, job.getRetryCount());
        assertTrue(!job.getUpdatedAt().isBefore(before),
                "updatedAt should be at or after the time incrementRetryCount was called");
    }

    @Test
    void shouldRetry_returnsTrueOnlyWhenStatusRetryAndBelowMaxRetries() {
        JobExecution job = new JobExecution();
        job.setStatus(JobExecution.JobStatus.RETRY);
        job.setRetryCount(0);
        job.setMaxRetries(2);

        assertTrue(job.shouldRetry(), "Should retry when status is RETRY and retryCount < maxRetries");

        job.setRetryCount(2);
        assertFalse(job.shouldRetry(), "Should not retry when retryCount == maxRetries");

        job.setStatus(JobExecution.JobStatus.FAILED);
        job.setRetryCount(0);
        assertFalse(job.shouldRetry(), "Should not retry when status is not RETRY");
    }

    @Test
    void markInProgress_setsStatusAndTimestamps() {
        JobExecution job = new JobExecution();
        job.setStatus(JobExecution.JobStatus.PENDING);

        Instant before = Instant.now();
        job.markInProgress();

        assertEquals(JobExecution.JobStatus.IN_PROGRESS, job.getStatus());
        assertNotNull(job.getLastAttempt());
        assertNotNull(job.getUpdatedAt());
        assertTrue(!job.getLastAttempt().isBefore(before));
        assertTrue(!job.getUpdatedAt().isBefore(before));
    }

    @Test
    void markCompleted_setsStatusResultAndUpdatedAt() {
        JobExecution job = new JobExecution();
        job.setStatus(JobExecution.JobStatus.IN_PROGRESS);
        job.setUpdatedAt(Instant.now().minusSeconds(30));

        Instant before = Instant.now();
        job.markCompleted("OK");

        assertEquals(JobExecution.JobStatus.COMPLETED, job.getStatus());
        assertEquals("OK", job.getResult());
        assertTrue(!job.getUpdatedAt().isBefore(before));
    }

    @Test
    void markFailed_whenRetriesRemain_incrementsRetryAndSchedulesNextAttemptUsingBaselineNow() {
        JobExecution job = new JobExecution();
        job.setStatus(JobExecution.JobStatus.IN_PROGRESS);
        job.setRetryCount(0);
        job.setMaxRetries(3);
        job.setLastAttempt(null);

        Instant before = Instant.now();
        job.markFailed("failure");

        assertEquals("failure", job.getErrorMessage());
        assertEquals(1, job.getRetryCount());
        assertEquals(JobExecution.JobStatus.RETRY, job.getStatus());
        assertNotNull(job.getUpdatedAt());
        assertNotNull(job.getNextAttempt());

        assertTrue(!job.getUpdatedAt().isBefore(before));

        long secondsBetween = Duration.between(job.getUpdatedAt(), job.getNextAttempt()).getSeconds();
        assertEquals(5L, secondsBetween, "First retry should use 5-second backoff");
    }

    @Test
    void markFailed_whenRetriesRemain_usesLastAttemptAsBaselineIfClockSkewsBackwards() {
        JobExecution job = new JobExecution();
        job.setStatus(JobExecution.JobStatus.IN_PROGRESS);
        job.setRetryCount(1); // second retry
        job.setMaxRetries(3);

        Instant futureLastAttempt = Instant.now().plusSeconds(3600);
        job.setLastAttempt(futureLastAttempt);

        job.markFailed("error with skew");

        assertEquals(2, job.getRetryCount());
        assertEquals(futureLastAttempt, job.getUpdatedAt(), "updatedAt should use lastAttempt as baseline when now is before it");
        assertEquals(JobExecution.JobStatus.RETRY, job.getStatus());
        assertNotNull(job.getNextAttempt());

        long secondsBetween = Duration.between(futureLastAttempt, job.getNextAttempt()).getSeconds();
        assertEquals(25L, secondsBetween, "Second retry should use 25-second backoff");
    }

    @Test
    void markFailed_whenNoRetriesRemain_setsStatusFailedWithoutChangingRetryCountOrNextAttempt() {
        JobExecution job = new JobExecution();
        job.setStatus(JobExecution.JobStatus.IN_PROGRESS);
        job.setRetryCount(3);
        job.setMaxRetries(3);
        job.setNextAttempt(null);

        job.markFailed("final failure");

        assertEquals("final failure", job.getErrorMessage());
        assertEquals(3, job.getRetryCount(), "Retry count should not change when no retries remain");
        assertEquals(JobExecution.JobStatus.FAILED, job.getStatus());
        assertNull(job.getNextAttempt(), "Next attempt should remain null when no retries remain");
    }

    @Test
    void jobTypeEnum_andJobStatusEnum_basicBehavior() {
        JobExecution.JobType[] jobTypes = JobExecution.JobType.values();
        assertArrayEquals(new JobExecution.JobType[]{
                        JobExecution.JobType.INVOICE_GENERATION,
                        JobExecution.JobType.REFUND_PROCESSING
                },
                jobTypes);

        assertEquals(JobExecution.JobType.INVOICE_GENERATION,
                JobExecution.JobType.valueOf("INVOICE_GENERATION"));
        assertEquals(JobExecution.JobType.REFUND_PROCESSING,
                JobExecution.JobType.valueOf("REFUND_PROCESSING"));

        JobExecution.JobStatus[] statuses = JobExecution.JobStatus.values();
        assertArrayEquals(new JobExecution.JobStatus[]{
                        JobExecution.JobStatus.PENDING,
                        JobExecution.JobStatus.IN_PROGRESS,
                        JobExecution.JobStatus.COMPLETED,
                        JobExecution.JobStatus.FAILED,
                        JobExecution.JobStatus.RETRY
                },
                statuses);

        assertEquals(JobExecution.JobStatus.PENDING,
                JobExecution.JobStatus.valueOf("PENDING"));
        assertEquals(JobExecution.JobStatus.RETRY,
                JobExecution.JobStatus.valueOf("RETRY"));
    }
}

