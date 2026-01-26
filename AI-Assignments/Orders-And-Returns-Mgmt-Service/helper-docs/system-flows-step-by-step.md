# Step-by-Step System Flows: Order & Returns Management System

This document lists all major flows in the system, each represented in a step-by-step diagrammatic format.
**Note:** All database operations (State History DB, Order DB, Return DB) use PostgreSQL for relational data storage and audit logging.

---

## 1. Order Lifecycle Flow

```
Customer
   |
   |--(Place Order)--> 
Order Service
   |
   |--(Create Initial DB Entry: State = PENDING_PAYMENT)--> 
State History DB
   |
   |--(Payment Received)--> 
Order Service
   |--(Update DB Entry: State = PAID)--> 
State History DB
   |
   |--(Warehouse Processing)--> 
Order Service
   |--(Update DB Entry: State = PROCESSING_IN_WAREHOUSE)--> 
State History DB
   |
   |--(Order Shipped)--> 
Order Service
   |--(Update DB Entry: State = SHIPPED)--> 
State History DB
   |
   |--(Order Delivered)--> 
Order Service
   |--(Update DB Entry: State = DELIVERED)--> 
State History DB
   |
   |--(Recovery: On service restart, pick up any transaction in progress from DB)--> 
```

---

## 2. Order Cancellation Flow

```
Customer
   |
   |--(Cancel Order: Only if PENDING_PAYMENT or PAID)--> 
Order Service
   |
   |--(Update DB Entry: State = CANCELLED)--> 
State History DB
   |
   |--(Recovery: On service restart, pick up any cancelled transaction from DB)--> 
```

---

## 3. Return Request Flow

```
Customer
   |
   |--(Initiate Return: Only if Order is DELIVERED)--> 
Return Service
   |
   |--(Create Initial DB Entry: State = REQUESTED)--> 
State History DB
   |
   |--(Manager Review)--> 
Return Service
   |--(Update DB Entry: State = APPROVED or REJECTED)--> 
State History DB
   |
   |--(If APPROVED: Customer Ships Item)--> 
Return Service
   |--(Update DB Entry: State = IN_TRANSIT)--> 
State History DB
   |
   |--(Warehouse Receives Item)--> 
Return Service
   |--(Update DB Entry: State = RECEIVED)--> 
State History DB
   |
   |--(Refund Processed)--> 
Return Service
   |--(Update DB Entry: State = COMPLETED)--> 
State History DB
   |
   |--(Recovery: On service restart, pick up any return in progress from DB)--> 
```

---

## 4. PDF Invoice Generation (Background Job)

```
Order Service
   |
   |--(Order status changes to SHIPPED)--> 
   |--(Update DB Entry: State = SHIPPED)--> 
State History DB
   |
   |--(Persist PDF Invoice Generation Job in job_execution table)--> 
JobExecution (DB-backed Job Queue)
   |
   |--(Background Job Processor picks up job)--> 
Background Job Processor
   |
   |--(Generate PDF Invoice)--> 
   |--(Send Invoice Email to Customer)--> 
   |--(Log Job Completion / Errors in DB)--> 
   |--(Recovery: On restart, JobScheduler picks up any PENDING/RETRY jobs from DB)--> 
State History DB
```

---

## 5. Refund Processing (Background Job)

```
Return Service
   |
   |--(Return status changes to COMPLETED)--> 
   |--(Update DB Entry: State = COMPLETED)--> 
State History DB
   |
   |--(Persist Refund Processing Job in job_execution table)--> 
JobExecution (DB-backed Job Queue)
   |
   |--(Background Job Processor picks up job)--> 
Background Job Processor
   |
   |--(Call Mock Payment Gateway API)--> 
   |--(Log Refund Result in DB via StateHistory entries)--> 
   |--(Recovery: On restart, JobScheduler picks up any PENDING/RETRY jobs from DB)--> 
State History DB
```

---

## 6. State History Logging (Audit Trail)

```
Any Service (Order/Return/Background Job)
   |
   |--(State Change Event)--> 
   |--(Create or Update DB Entry for State Change)--> 
State History DB
   |
   |--(Recovery: On service restart, pick up any transaction in progress from DB)--> 
   |--(Audit Query/Reporting)--> 
Admin/Manager
```

---

**Update this file as new flows are added or existing flows evolve.**
