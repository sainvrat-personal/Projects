# Epic 3: Background Job Processing

## Story 3.1: PDF Invoice Generation
- **Task:** Integrate background job processor (Celery/Sidekiq/Hangfire)
  - **Output:** Working job processor setup and running
- **Task:** Implement job to generate PDF invoice on SHIPPED
  - **Output:** PDF invoice generated and stored/sent
- **Task:** Simulate emailing invoice to customer
  - **Output:** Email simulation with invoice attachment

## Story 3.2: Refund Processing
- **Task:** Implement job to process refund via mock payment gateway on COMPLETED return
  - **Output:** Refund API call and result logged
- **Task:** Log refund result
  - **Output:** Audit log entry for refund processing
