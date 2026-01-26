package com.example.orderservice.controller;

import com.example.orderservice.entity.JobExecution;
import com.example.orderservice.entity.JobExecution.JobStatus;
import com.example.orderservice.entity.JobExecution.JobType;
import com.example.orderservice.repository.JobExecutionRepository;
import com.example.orderservice.service.JobScheduler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(JobController.class)
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JobExecutionRepository jobExecutionRepository;

    @MockBean
    private JobScheduler jobScheduler;

    private UUID jobId;
    private UUID entityId;
    private JobExecution job;

    @BeforeEach
    void setUp() {
        jobId = UUID.randomUUID();
        entityId = UUID.randomUUID();

        job = new JobExecution();
        job.setId(jobId);
        job.setJobType(JobType.INVOICE_GENERATION);
        job.setEntityId(entityId);
        job.setStatus(JobStatus.PENDING);
        job.setRetryCount(1);
        job.setMaxRetries(3);
        job.setCreatedAt(Instant.now());
        job.setUpdatedAt(Instant.now());
    }

    // ---------------------------------------------------------------------
    // GET /jobs
    // ---------------------------------------------------------------------

    @Test
    void testGetAllJobs_returnsList() throws Exception {
        JobExecution otherJob = new JobExecution();
        otherJob.setId(UUID.randomUUID());
        otherJob.setJobType(JobType.REFUND_PROCESSING);
        otherJob.setEntityId(UUID.randomUUID());
        otherJob.setStatus(JobStatus.COMPLETED);

        when(jobExecutionRepository.findAll()).thenReturn(Arrays.asList(job, otherJob));

        mockMvc.perform(get("/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(job.getId().toString()))
                .andExpect(jsonPath("$[1].id").value(otherJob.getId().toString()));
    }

    @Test
    void testGetAllJobs_emptyList() throws Exception {
        when(jobExecutionRepository.findAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ---------------------------------------------------------------------
    // GET /jobs/{jobId}
    // ---------------------------------------------------------------------

    @Test
    void testGetJob_success() throws Exception {
        when(jobExecutionRepository.findById(jobId)).thenReturn(Optional.of(job));

        mockMvc.perform(get("/jobs/{jobId}", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(jobId.toString()))
                .andExpect(jsonPath("$.status").value(job.getStatus().name()));
    }

    @Test
    void testGetJob_notFound() throws Exception {
        when(jobExecutionRepository.findById(jobId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/jobs/{jobId}", jobId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.message").value("Job not found: " + jobId));
    }

    // ---------------------------------------------------------------------
    // GET /jobs/status/{status}
    // ---------------------------------------------------------------------

    @Test
    void testGetJobsByStatus_success() throws Exception {
        when(jobExecutionRepository.findByStatus(JobStatus.PENDING))
                .thenReturn(Collections.singletonList(job));

        mockMvc.perform(get("/jobs/status/{status}", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(jobId.toString()))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void testGetJobsByStatus_invalidStatus_returnsBadRequest() throws Exception {
        // Invalid enum value causes MethodArgumentTypeMismatchException before hitting controller
        mockMvc.perform(get("/jobs/status/{status}", "NOT_A_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").exists());

        verifyNoInteractions(jobExecutionRepository);
    }

    // ---------------------------------------------------------------------
    // GET /jobs/type/{type}
    // ---------------------------------------------------------------------

    @Test
    void testGetJobsByType_success() throws Exception {
        when(jobExecutionRepository.findByJobType(JobType.INVOICE_GENERATION))
                .thenReturn(Collections.singletonList(job));

        mockMvc.perform(get("/jobs/type/{type}", "INVOICE_GENERATION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(jobId.toString()))
                .andExpect(jsonPath("$[0].jobType").value("INVOICE_GENERATION"));
    }

    @Test
    void testGetJobsByType_invalidType_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/jobs/type/{type}", "NOT_A_TYPE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").exists());

        verifyNoInteractions(jobExecutionRepository);
    }

    // ---------------------------------------------------------------------
    // GET /jobs/type/{type}/status/{status}
    // ---------------------------------------------------------------------

    @Test
    void testGetJobsByTypeAndStatus_success() throws Exception {
        when(jobExecutionRepository.findByJobTypeAndStatus(JobType.INVOICE_GENERATION, JobStatus.PENDING))
                .thenReturn(Collections.singletonList(job));

        mockMvc.perform(get("/jobs/type/{type}/status/{status}", "INVOICE_GENERATION", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(jobId.toString()))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].jobType").value("INVOICE_GENERATION"));
    }

    // ---------------------------------------------------------------------
    // GET /jobs/entity/{entityId}
    // ---------------------------------------------------------------------

    @Test
    void testGetJobsByEntityId_success() throws Exception {
        when(jobExecutionRepository.findByEntityId(entityId))
                .thenReturn(Collections.singletonList(job));

        mockMvc.perform(get("/jobs/entity/{entityId}", entityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].entityId").value(entityId.toString()));
    }

    @Test
    void testGetJobsByEntityId_emptyList() throws Exception {
        when(jobExecutionRepository.findByEntityId(entityId))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/jobs/entity/{entityId}", entityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ---------------------------------------------------------------------
    // POST /jobs/{jobId}/retry
    // ---------------------------------------------------------------------

    @Test
    void testRetryJob_success() throws Exception {
        when(jobExecutionRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobExecutionRepository.save(any(JobExecution.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(post("/jobs/{jobId}/retry", jobId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.message").value("Job marked for retry successfully."))
                .andExpect(jsonPath("$.jobId").value(jobId.toString()))
                .andExpect(jsonPath("$.jobStatus").value("RETRY"));

        verify(jobExecutionRepository, times(1)).save(any(JobExecution.class));
    }

    @Test
    void testRetryJob_notFound() throws Exception {
        when(jobExecutionRepository.findById(jobId)).thenReturn(Optional.empty());

        mockMvc.perform(post("/jobs/{jobId}/retry", jobId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Job not found: " + jobId));

        verify(jobExecutionRepository, times(1)).findById(jobId);
        verify(jobExecutionRepository, times(0)).save(any(JobExecution.class));
    }

    // ---------------------------------------------------------------------
    // POST /jobs/process
    // ---------------------------------------------------------------------

    @Test
    void testProcessJobsOnce_success() throws Exception {
        doNothing().when(jobScheduler).processInvoiceJobs();
        doNothing().when(jobScheduler).processRefundJobs();
        doNothing().when(jobScheduler).processRetryJobs();

        mockMvc.perform(post("/jobs/process")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.message").value("Job processing triggered successfully."));

        verify(jobScheduler, times(1)).processInvoiceJobs();
        verify(jobScheduler, times(1)).processRefundJobs();
        verify(jobScheduler, times(1)).processRetryJobs();
    }

    @Test
    void testProcessJobsOnce_schedulerThrows_returnsServerError() throws Exception {
        doThrow(new RuntimeException("Scheduler failure"))
                .when(jobScheduler)
                .processInvoiceJobs();

        mockMvc.perform(post("/jobs/process")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));

        verify(jobScheduler, times(1)).processInvoiceJobs();
        // Once the first call fails, the others should not be invoked
        verify(jobScheduler, times(0)).processRefundJobs();
        verify(jobScheduler, times(0)).processRetryJobs();
    }
}

