package com.example.orderservice.service;

import com.example.orderservice.entity.JobExecution;
import com.example.orderservice.entity.JobExecution.JobType;
import com.example.orderservice.repository.JobExecutionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:jobschedulertest",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.profiles.active=test"
})
@Transactional
class JobSchedulerIdempotencyTest {

    @Autowired
    private JobScheduler jobScheduler;

    @Autowired
    private JobExecutionRepository jobExecutionRepository;

    @Test
    void scheduleInvoiceJob_isIdempotentForSameOrder() {
        UUID orderId = UUID.randomUUID();

        JobExecution first = jobScheduler.scheduleInvoiceJob(orderId);
        JobExecution second = jobScheduler.scheduleInvoiceJob(orderId);

        assertNotNull(first.getId());
        assertEquals(first.getId(), second.getId(), "Expected the same job to be reused for the same order");

        List<JobExecution> jobs = jobExecutionRepository.findByEntityIdAndJobType(orderId, JobType.INVOICE_GENERATION);
        assertEquals(1, jobs.size(), "Expected exactly one invoice job for a given order");
        assertEquals("INVOICE-" + orderId, jobs.get(0).getIdempotencyKey());
    }

    @Test
    void scheduleRefundJob_isIdempotentForSameReturn() {
        UUID returnId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID(); // only used to derive the idempotency domain

        JobExecution first = jobScheduler.scheduleRefundJob(returnId, orderId);
        JobExecution second = jobScheduler.scheduleRefundJob(returnId, orderId);

        assertNotNull(first.getId());
        assertEquals(first.getId(), second.getId(), "Expected the same job to be reused for the same return");

        List<JobExecution> jobs = jobExecutionRepository.findByEntityIdAndJobType(returnId, JobType.REFUND_PROCESSING);
        assertEquals(1, jobs.size(), "Expected exactly one refund job for a given return");
        assertEquals("REFUND-" + returnId, jobs.get(0).getIdempotencyKey());
    }
}

