package com.example.orderservice.repository;

import com.example.orderservice.entity.JobExecution;
import com.example.orderservice.entity.JobExecution.JobStatus;
import com.example.orderservice.entity.JobExecution.JobType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobExecutionRepository extends JpaRepository<JobExecution, UUID> {
    
    List<JobExecution> findByStatus(JobStatus status);
    
    List<JobExecution> findByJobTypeAndStatus(JobType jobType, JobStatus status);

    /**
     * Find all jobs for a given job type, regardless of status.
     * <p>
     * This is primarily used by operational/monitoring endpoints where the
     * caller wants to see the complete history for a job type without having
     * to specify a particular status.
     */
    List<JobExecution> findByJobType(JobType jobType);
    
    Optional<JobExecution> findByIdempotencyKey(String idempotencyKey);
    
    List<JobExecution> findByEntityIdAndJobType(UUID entityId, JobType jobType);

    /**
     * Find all jobs that are associated with the given business entity
     * (order, return, etc.), regardless of job type.
     */
    List<JobExecution> findByEntityId(UUID entityId);
    
    @Query("SELECT j FROM JobExecution j WHERE j.status = :status AND j.nextAttempt <= :now")
    List<JobExecution> findJobsForRetry(@Param("status") JobStatus status, @Param("now") Instant now);
    
    @Query("SELECT j FROM JobExecution j WHERE j.status = :status AND j.jobType = :jobType ORDER BY j.createdAt ASC")
    List<JobExecution> findPendingJobsOrderedByCreatedAt(@Param("status") JobStatus status, @Param("jobType") JobType jobType);
}