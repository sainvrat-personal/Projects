package com.example.orderservice.controller;

import com.example.orderservice.entity.JobExecution;
import com.example.orderservice.entity.JobExecution.JobStatus;
import com.example.orderservice.entity.JobExecution.JobType;
import com.example.orderservice.repository.JobExecutionRepository;
import com.example.orderservice.service.JobScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/jobs")
public class JobController {
    
    private final JobExecutionRepository jobExecutionRepository;
    private final JobScheduler jobScheduler;
    
    @Autowired
    public JobController(JobExecutionRepository jobExecutionRepository, JobScheduler jobScheduler) {
        this.jobExecutionRepository = jobExecutionRepository;
        this.jobScheduler = jobScheduler;
    }
    
    /**
     * Get all jobs
     * @return List of all jobs
     */
    @GetMapping
    public ResponseEntity<?> getAllJobs() {
        List<JobExecution> jobs = jobExecutionRepository.findAll();
        return ResponseEntity.ok(jobs);
    }
    
    /**
     * Get a job by ID
     * @param jobId The job ID
     * @return The job
     */
    @GetMapping("/{jobId}")
    public ResponseEntity<?> getJob(@PathVariable UUID jobId) {
        JobExecution job = jobExecutionRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        return ResponseEntity.ok(job);
    }
    
    /**
     * Get jobs by status
     * @param status The job status
     * @return List of jobs with the given status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<?> getJobsByStatus(@PathVariable JobStatus status) {
        List<JobExecution> jobs = jobExecutionRepository.findByStatus(status);
        return ResponseEntity.ok(jobs);
    }
    
    /**
     * Get jobs by type
     * @param type The job type
     * @return List of jobs with the given type
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<?> getJobsByType(@PathVariable JobType type) {
        List<JobExecution> jobs = jobExecutionRepository.findByJobType(type);
        return ResponseEntity.ok(jobs);
    }
    
    /**
     * Get jobs by type and status
     * @param type The job type
     * @param status The job status
     * @return List of jobs with the given type and status
     */
    @GetMapping("/type/{type}/status/{status}")
    public ResponseEntity<?> getJobsByTypeAndStatus(
            @PathVariable JobType type,
            @PathVariable JobStatus status) {
        List<JobExecution> jobs = jobExecutionRepository.findByJobTypeAndStatus(type, status);
        return ResponseEntity.ok(jobs);
    }
    
    /**
     * Get jobs for an entity
     * @param entityId The entity ID
     * @return List of jobs for the given entity
     */
    @GetMapping("/entity/{entityId}")
    public ResponseEntity<?> getJobsByEntityId(@PathVariable UUID entityId) {
        List<JobExecution> jobs = jobExecutionRepository.findByEntityId(entityId);
        return ResponseEntity.ok(jobs);
    }

    /**
     * Manually mark a job as eligible for retry and schedule it for immediate
     * processing by the retry worker.
     * <p>
     * This does not create a new job record; instead it reuses the existing
     * {@link JobExecution} entry so that idempotency guarantees (especially
     * around refund processing) are preserved. Operators can safely invoke
     * this even for {@code COMPLETED} jobs – refund jobs will no-op thanks to
     * their upstream idempotency checks, and invoice jobs will simply
     * regenerate an invoice if needed.
     */
    @PostMapping("/{jobId}/retry")
    public ResponseEntity<?> retryJob(@PathVariable UUID jobId) {
        JobExecution job = jobExecutionRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        // Set the job back to RETRY and make it immediately eligible for the
        // scheduled retry worker. We intentionally leave retryCount unchanged
        // so that manual intervention can override the automatic max-retries
        // guard without creating a brand new job record.
        job.setStatus(JobStatus.RETRY);
        job.setNextAttempt(java.time.Instant.now());
        jobExecutionRepository.save(job);

        Map<String, Object> result = new HashMap<>();
        result.put("status", "OK");
        result.put("message", "Job marked for retry successfully.");
        result.put("jobId", job.getId());
        result.put("jobStatus", job.getStatus());
        return ResponseEntity.ok(result);
    }

    /**
     * Manually trigger processing of pending and retryable jobs.
     * <p>
     * This provides an operational escape hatch if scheduled execution is
     * misconfigured or disabled: operators or test harnesses can invoke this
     * endpoint to ensure PENDING / RETRY jobs do not languish indefinitely.
     */
    @PostMapping("/process")
    public ResponseEntity<?> processJobsOnce() {
        jobScheduler.processInvoiceJobs();
        jobScheduler.processRefundJobs();
        jobScheduler.processRetryJobs();

        Map<String, Object> result = new HashMap<>();
        result.put("status", "OK");
        result.put("message", "Job processing triggered successfully.");
        return ResponseEntity.ok(result);
    }
}