# Architecture Overview: Order & Returns Management System

This document describes the high-level architecture for the backend application powering the Order and Returns Management System. This document reflects the current implementation of the system.

---

## 1. System Components
- **Order Management Module**
  - Implements a state machine for order lifecycle with enum-based status: PENDING_PAYMENT → PAID → PROCESSING_IN_WAREHOUSE → SHIPPED → DELIVERED
  - Supports cancellation from PENDING_PAYMENT or PAID states with required cancellation reason
  - Logs all state transitions for auditing via StateHistory entity

- **Returns Management Module**
  - Implements a state machine for returns: REQUESTED → APPROVED/REJECTED → IN_TRANSIT → RECEIVED → COMPLETED
  - Only allows returns for DELIVERED orders
  - Logs all state transitions for auditing with ReturnStateChange and StateHistory entities

- **Background Job Processor**
  - Handles asynchronous tasks using Spring's scheduling capabilities:
    - PDF invoice generation when order is SHIPPED
    - Email notification for invoice delivery
    - Refund processing via mock payment gateway when return is COMPLETED
  - Uses Spring's @Scheduled annotation for job scheduling and monitoring

- **Third-Party Integrations**
  - Mock payment gateway API for refund processing
  - Email service for notifications and invoice delivery
  - Uses Spring Mail for email functionality

- **Database (PostgreSQL)**
  - Uses PostgreSQL for robust relational data storage
  - Entity structure: Order, Return, StateHistory, ReturnStateChange
  - Schema designed for tracking state changes and maintaining audit history

- **API Layer**
  - RESTful endpoints for managing orders, returns, and querying state histories
  - Well-defined request/response DTOs
  - Consistent error handling across all endpoints

- **Swagger-UI**
  - Exposes API documentation via SpringDoc OpenAPI
  - Accessible via `/swagger-ui.html` and `/api-docs`

- **Docker Containerization**
  - Multi-stage Docker build for application packaging
  - Docker Compose orchestration for application and database
  - Configuration via environment variables

---

## 2. Key Architectural Decisions
- **Enum-based State Machines**: Order and Return status implemented as Java enums with explicit validation of state transitions.
- **Transaction Management**: ACID transactions for critical operations using Spring's @Transactional.
- **Audit Logging**: Persistent tracking of all state transitions with timestamp, user info, and state changes.
- **Email Integration**: Configurable email service for notifications and documents delivery.
- **OpenAPI Documentation**: Automatic API documentation via SpringDoc.
- **Containerization**: Docker-based deployment for consistent environment across development and production.
- **Environment-specific Profiles**: Different configuration profiles for local, docker, and other environments.

---

## 3. System Implementation Details
- **Spring Boot Framework**: Core application framework
- **Spring Data JPA**: Database access layer with repository pattern
- **PostgreSQL**: Primary database
- **Java 17**: Programming language
- **Maven**: Build and dependency management
- **JUnit 5**: Testing framework
- **Spring Mail**: Email service integration
- **Lombok**: Reduces boilerplate code
- **OpenAPI/Swagger**: API documentation

---

**Related Documents:** 
- See `db/schema-diagram.md` for database schema details
- See `APIs.md` for API endpoint documentation
- See `docker-compose.yml` for service configuration
