# Background Job Processing System

This document explains the background job processing system used in the Order & Returns Management application.

## Overview

The application includes a robust, self-healing background job processing system for handling asynchronous tasks such as:

- Generating PDF invoices when orders are shipped
- Processing refunds when returns are approved
- Sending email notifications at various stages of the order/return lifecycle

The design intentionally combines:

- **A database-backed worker/queue** for critical, auditable work (invoices, refunds)
- **A clear `@Async` strategy** for fire-and-forget flows that can tolerate best-effort delivery

## Architecture

Instead of relying on external message queues, the application uses a **database-backed job queue** (`job_execution` table) together with Spring's **scheduling** and **`@Async`** capabilities for simplicity. This approach provides:

- Job persistence across application restarts
- Job status tracking and monitoring via REST APIs
- Automatic retry with exponential backoff
- Idempotent job processing

### Component Diagram

```
┌─────────────────────┐      ┌──────────────────┐      ┌─────────────────────┐
│                     │      │                  │      │                     │
│  Service Layer      │─────▶│  Job Scheduler   │─────▶│  Job Processor      │
│  (creates jobs)     │      │  (finds & runs)  │      │  (executes jobs)    │
│                     │      │                  │      │                     │
└─────────────────────┘      └──────────────────┘      └─────────────────────┘
         │                            │                          │
         │                            │                          │
         ▼                            ▼                          ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                                                                             │
│                              Database                                       │
│                          (job_execution table)                              │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Job Lifecycle

1. **Job Creation**
   - Service layer (e.g. order/return flows) creates a record in `job_execution` with status `PENDING`.
   - Each job has:
     - `job_type` (e.g. `INVOICE_GENERATION`, `REFUND_PROCESSING`)
     - `entity_id` (order ID or return ID)
     - a unique `idempotency_key` to prevent duplicate jobs for the same business action.

2. **Job Scheduling / Worker**
   - `JobScheduler` runs periodically (Spring `@Scheduled`):
     - `processInvoiceJobs` and `processRefundJobs` pick up `PENDING` jobs.
     - `processRetryJobs` picks up `RETRY` jobs whose `next_attempt` is due.
   - An optimistic-locking `version` column on `job_execution` ensures **only one worker node** can move a job from `PENDING` to `IN_PROGRESS`, preventing duplicate processing in a multi-instance deployment.

3. **Job Processing**
   - The scheduler delegates to synchronous workers:
     - `InvoiceJobService.generateInvoice(...)`
     - `RefundJobService.processRefund(...)`
   - On success, the job is marked `COMPLETED` and an optional `result` message is stored.

4. **Failure Handling and Retry**
   - Any unexpected failure inside a worker throws an exception.
   - The scheduler catches this and calls `job.markFailed(errorMessage)`, which:
     - increments `retry_count` (if below `max_retries`)
     - computes `next_attempt` using exponential backoff
     - moves status to `RETRY` (or `FAILED` once max retries are exhausted).

5. **Manual Intervention**
   - Operators can:
     - Inspect job status and details via `/jobs` endpoints.
     - Trigger a one-off processing pass via `POST /jobs/process`.
     - Mark a specific job for **manual retry** via `POST /jobs/{jobId}/retry`.

## Database Schema

The `job_execution` table stores all job-related information. The Java entity lives in `JobExecution.java`; at a high level it contains:

- `id` (`UUID`): primary key
- `version` (`Long`): optimistic locking version
- `job_type` (`JobType` enum)
- `entity_id` (`UUID`): order/return ID
- `status` (`JobStatus` enum: `PENDING`, `IN_PROGRESS`, `COMPLETED`, `FAILED`, `RETRY`)
- `retry_count` / `max_retries`
- `last_attempt` / `next_attempt`
- `error_message` and `result`
- `created_at` / `updated_at`
- `idempotency_key` (unique)

## Job Types

### 1. Invoice Generation Jobs

Created when an order transitions to `SHIPPED` state:

```json
{
  "jobType": "INVOICE_GENERATION",
  "referenceId": "ORDER-12345",
  "payload": {
    "orderId": "ORDER-12345",
    "customerEmail": "customer@example.com",
    "sendEmail": true
  }
}
```

### 2. Refund Processing Jobs

Created when a return transitions to `COMPLETED` state:

```json
{
  "jobType": "REFUND_PROCESSING",
  "referenceId": "RETURN-54321",
  "payload": {
    "returnId": "RETURN-54321",
    "orderId": "ORDER-12345",
    "amount": 99.99,
    "paymentId": "PAYMENT-67890"
  }
}
```

## Job States

- `PENDING`: Job created but not yet processed
- `IN_PROGRESS`: Job currently being processed
- `COMPLETED`: Job successfully completed
- `FAILED`: Job failed after all retry attempts
- `RETRY`: Job failed but scheduled for retry

## Retry Strategy

The system uses an **exponential backoff** strategy implemented in `JobExecution.markFailed`:

- For each failure while `retry_count < max_retries`:
  - `retry_count` is incremented.
  - `next_attempt` is set to `last_attempt + delay`, where:
    - delay (seconds) = \(5^{retry\_count}\) → 5s, 25s, 125s, ...
  - status moves to `RETRY`.
- Once `retry_count` reaches `max_retries`, status is set to `FAILED` and the job is no longer picked up automatically.

At the database-access layer, all scheduler methods are also annotated with `@Retryable` so that **transient database errors** (e.g. `TransientDataAccessException`) are retried before failing the job itself.

## Idempotency

All job processors are designed to be **idempotent**, meaning they can be safely retried without causing duplicate side effects.

At the **job level**:

- Each job is created with a deterministic `idempotency_key`:
  - Invoices: `"INVOICE-" + orderId`
  - Refunds: `"REFUND-" + returnId`
- The `JobExecutionRepository` enforces uniqueness of this key.
- The scheduler first looks up any existing job for the same entity and type; if a non-`FAILED` job already exists, it is **reused** instead of creating a new one.

At the **business level**:

- **Refunds**:
  - `RefundJobService` checks `StateHistory` for an existing `"REFUNDED"` entry for the given return.
  - If found, the refund path becomes a no-op even if the job is retried or manually re-triggered.
  - A deterministic `IdempotencyRef` (`transactionId:returnId`) is logged so external gateways can be reconciled.
- **Invoices**:
  - Only a single `INVOICE_GENERATION` job is created per order (enforced by `idempotency_key`).
  - Regenerating an invoice is safe: it creates a fresh PDF and (optionally) resends email, but does not mutate core order or payment state.

## Monitoring and Operational Controls

The system provides several endpoints for monitoring job status and controlling execution:

- **List / inspect jobs**
  - `GET /jobs` – list all jobs.
  - `GET /jobs/{jobId}` – inspect a single job (including `status`, `retryCount`, `errorMessage`, `result`).
  - `GET /jobs/status/{status}` – list jobs by `JobStatus`.
  - `GET /jobs/type/{type}` – list jobs by `JobType`.
  - `GET /jobs/type/{type}/status/{status}` – list jobs by both type and status.
  - `GET /jobs/entity/{entityId}` – list jobs for a specific order/return.

- **Trigger processing**
  - `POST /jobs/process`
    - Immediately runs the three scheduled workers once:
      - `processInvoiceJobs`
      - `processRefundJobs`
      - `processRetryJobs`
    - Useful when testing or when scheduling is temporarily disabled/misconfigured.

- **Manual retry**
  - `POST /jobs/{jobId}/retry`
    - Marks the job as `RETRY` and sets `nextAttempt` to now so that it is picked up by the retry worker.
    - Reuses the same `JobExecution` record to preserve idempotency guarantees, especially for refunds.

## Implementation Details and `@Async` Strategy

### Worker / Queue Implementation

- `JobExecution` (entity) models the durable queue.
- `JobExecutionRepository` exposes finder methods for:
  - status, type, entity, idempotency key, and retry candidates.
- `JobScheduler`:
  - Creates jobs (`scheduleInvoiceJob`, `scheduleRefundJob`) with proper idempotency keys.
  - Periodically processes:
    - `processInvoiceJobs` for invoice generation.
    - `processRefundJobs` for refund processing.
    - `processRetryJobs` for `RETRY` jobs whose `next_attempt` is due.
  - Uses `@Retryable` on all scheduling/worker methods to handle transient DB problems.

### `@Async` Strategy

The system uses `@Async` for non-critical, fire-and-forget entry points while keeping the **authoritative worker logic synchronous** so that failures can be surfaced and retried:

- Configuration:
  - `AsyncConfig` defines:
    - a general-purpose `taskExecutor`
    - a dedicated `invoiceJobExecutor`
    - a dedicated `refundJobExecutor`
  - `@EnableAsync` and `@EnableRetry` are enabled at configuration level.

- Invoice flow:
  - `InvoiceJobService.generateInvoiceAsync(...)` is annotated with `@Async("invoiceJobExecutor")`.
  - It delegates to `generateInvoice(...)` which:
    - generates the PDF,
    - sends the email (if a valid recipient exists),
    - throws a `RuntimeException` for any failure so the scheduler can mark the job as `FAILED/RETRY`.

- Refund flow:
  - `RefundJobService.processRefundAsync(...)` is annotated with `@Async("refundJobExecutor")`.
  - It delegates to `processRefund(...)` which:
    - performs all validation and idempotency checks,
    - calls the mock payment gateway,
    - records detailed audit entries in `StateHistory`,
    - throws on transient/unknown failures so jobs can be retried.

In other words:

- **Business correctness and idempotency** live in the synchronous methods.
- **`@Async`** is a convenience wrapper for callers that do not need to wait for job completion, while the **database-backed worker/queue** is responsible for retries and backoff.

## How Failures Are Surfaced

Failures are made visible through multiple channels:

- **JobExecution fields**
  - `status` indicates whether a job is `PENDING`, `IN_PROGRESS`, `RETRY`, `FAILED`, or `COMPLETED`.
  - `retry_count`, `max_retries`, and `next_attempt` show retry/backoff behavior.
  - `error_message` contains a human-readable reason for the last failure.
  - `result` contains a short success message when available.

- **Audit log (refunds)**
  - `RefundJobService` writes `StateHistory` rows with states such as:
    - `REFUNDED`, `REFUND_DECLINED`, `REFUND_STATUS_UNKNOWN`, `REFUND_AUTH_ERROR`, `REFUND_RATE_LIMITED`.
  - These provide a durable, queryable history of refund attempts and outcomes even if the job itself has been retried multiple times.

- **Application logs**
  - All workers use `Slf4j` logging with clear context (job ID, order/return ID, transaction ID).
  - Async wrappers (`generateInvoiceAsync`, `processRefundAsync`) catch and log exceptions so that background failures are never completely silent.

- **HTTP error responses**
  - When a client calls a job-related endpoint with an invalid ID or path, the global error handler produces a standardized `ErrorResponse` (see `ERROR_HANDLING.md`).
  - For core order/return APIs, synchronous errors (e.g. invalid state transition, missing entities) are surfaced immediately as 4xx/5xx responses, while background failures later appear via the `/jobs` monitoring and refund audit trails described above.

## Best Practices for Production Deployment

1. **Monitor Job Queue Size**: 
   - Set up alerts for growing queue size
   - Consider scaling horizontally if queue growth is consistent

2. **Set Appropriate Timeouts**:
   - Configure timeouts for external service calls
   - Ensure jobs can't run indefinitely

3. **Add Circuit Breakers**:
   - Implement circuit breakers for external dependencies
   - Prevent cascading failures

4. **Implement Dead Letter Queue**:
   - Move persistently failing jobs to a separate table
   - Allows operational review without affecting normal processing

5. **Add Metrics Collection**:
   - Track job processing time
   - Monitor success/failure rates
   - Set up dashboards for job processing metrics