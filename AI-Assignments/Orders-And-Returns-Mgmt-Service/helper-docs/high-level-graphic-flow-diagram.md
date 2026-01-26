# High-Level Graphic Flow Diagram: Order & Returns Management System

This diagram illustrates the interaction between major services/components, including background job processing and message queues.

---

```
+-------------------+         +-------------------+         +-------------------+
|                   |         |                   |         |                   |
|   API Layer       +-------->+  Order Service    +-------->+  State History DB |
| (REST/Swagger-UI) |         |                   |         |                   |
+-------------------+         +-------------------+         +-------------------+
         |                             |                             ^
         |                             v                             |
         |                   +-------------------+                   |
         |                   |                   |                   |
         |                   |  Return Service   +-------------------+
         |                   |                   |                   |
         |                   +-------------------+                   |
         |                             |                             |
         v                             v                             |
+-------------------+         +-------------------+         +-------------------+
|                   |         |                   |         |                   |
| Background Job    |<--------+  Job Queue/       +<--------+  Third-Party      |
| Processor         |         |  Message Queue    |         |  Integrations     |
| (Celery, etc.)    |         | (RabbitMQ, etc.) |         | (Email, Payment)  |
+-------------------+         +-------------------+         +-------------------+
```

---

## Example: PDF Invoice Generation (Background Job)

```
Order Service
   |
   |--(Order status changes to SHIPPED)-->
   |
Job Queue / Message Queue (e.g., RabbitMQ, Celery, etc.)
   |
   |--(Enqueue PDF Invoice Generation Job)-->
   |
Background Job Processor
   |
   |--(Generate PDF Invoice)-->
   |
   |--(Send Invoice Email to Customer)-->
   |
   |--(Log Job Completion / Errors)-->
```

---

## Example: Refund Processing (Background Job)

```
Return Service
   |
   |--(Return status changes to COMPLETED)-->
   |
Job Queue / Message Queue
   |
   |--(Enqueue Refund Processing Job)-->
   |
Background Job Processor
   |
   |--(Call Mock Payment Gateway API)-->
   |
   |--(Log Refund Result)-->
```

---

**Update this diagram as the architecture evolves or new components are added.**
