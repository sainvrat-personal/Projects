# Project Structure Documentation

This document provides a comprehensive overview of the Order & Returns Management System's project structure, explaining the purpose and organization of each directory and key module.

## Overview

The project follows a standard Spring Boot application structure with additional organization for documentation, database migrations, containerization, and comprehensive testing. The architecture supports clean separation of concerns, scalability, and maintainability.

## Root Directory Structure

```
order-returns-management/
├── README.md                           # Main project documentation
├── pom.xml                            # Maven build configuration and dependencies
├── Dockerfile                         # Container image definition
├── docker-compose.yml                 # Multi-container setup for development
├── APIs Documentation/                # API-related documentation
├── Architecture Documentation/        # System design and workflow docs
├── Configuration Documentation/       # Setup and configuration guides
├── Database Schema/                   # Database-related files
├── Source Code/                       # Application source code
├── Tests/                            # Test suites and coverage reports
└── Build Artifacts/                  # Compiled outputs and reports
```

## Documentation Files

### Core Documentation
- **README.md** - Comprehensive project overview, setup instructions, and usage guide
- **PROJECT_STRUCTURE.md** - This file, explaining project organization
- **WORKFLOW_DESIGN.md** - State machine diagrams and implementation details

### API Documentation
- **API-SPECIFICATION.yml** - OpenAPI 3.0 specification for all endpoints
- **apis.md** - Detailed API endpoint documentation with examples
- **TESTING_STRATEGY.md** - Testing approach and coverage strategy

### Configuration and Setup
- **EMAIL_CONFIG.md** - Email service configuration and troubleshooting
- **ERROR_HANDLING.md** - Standardized error response documentation
- **BACKGROUND_JOBS.md** - Background job processing system guide
- **STATE_TRANSITIONS.md** - Order and return state machine documentation

### Architecture and Design
- **architecture.md** - High-level system architecture
- **high-level-graphic-flow-diagram.md** - Visual system flow diagrams
- **system-flows-step-by-step.md** - Detailed workflow descriptions
- **Instructions.md** - Development and deployment instructions
- **project-initialization-steps.md** - Project setup checklist

### Development and Quality
- **Testing & Quality Assurance.md** - QA processes and standards
- **CHAT_HISTORY.md** - Development history and decision log
- **feedback.txt** - Development feedback and improvements
- **prd.md** - Product requirements documentation

## Build Configuration

### Maven Configuration
- **pom.xml** - Maven project object model containing:
  - Project metadata and versioning
  - Dependency management (Spring Boot, PostgreSQL, Testing frameworks)
  - Build plugins (Surefire, JaCoCo, Spring Boot Maven plugin)
  - Profiles for different environments (local, docker, test)

### Containerization
- **Dockerfile** - Multi-stage Docker build configuration:
  - Build stage using Maven for compilation
  - Runtime stage with Eclipse Temurin JDK 17 for execution
  - Optimized for containerized deployment

- **docker-compose.yml** - Development environment setup:
  - Application container configuration
  - PostgreSQL database container
  - Environment variable management
  - Network and volume configurations

## Database Schema (`db/`)

```
db/
├── schema-diagram.md                   # Visual database schema documentation
└── migration/
    └── V1__init_schema.sql            # Initial database schema creation
```

### Database Migration
- **V1__init_schema.sql** - Complete initial schema including:
  - `orders` table with order lifecycle data
  - `returns` table for return processing
  - `state_history` table for audit trail
  - `return_state_history` table for return-specific tracking
  - `job_execution` table for background job management
  - Proper indexes for query optimization
  - Foreign key relationships and constraints

## Source Code (`src/`)

### Application Source (`src/main/java/com/example/orderservice/`)

```
src/main/java/com/example/orderservice/
├── OrderReturnsManagementApplication.java  # Spring Boot main application class
├── config/                                # Configuration classes
│   ├── AsyncConfig.java                   # Async processing configuration
│   ├── EmailConfig.java                   # Email-related configuration
│   ├── RestTemplateConfig.java            # REST client configuration
│   └── SwaggerConfig.java                 # API documentation configuration
├── controller/                            # REST API controllers
│   ├── OrderController.java               # Order management endpoints
│   ├── ReturnController.java              # Return processing endpoints
│   ├── JobController.java                 # Background job monitoring
│   └── MockPaymentController.java         # Mock payment service
├── service/                               # Business logic layer
│   ├── OrderService.java                  # Order lifecycle management
│   ├── ReturnService.java                 # Return processing logic
│   ├── InvoiceJobService.java             # Invoice generation service
│   ├── RefundJobService.java              # Refund processing service
│   ├── EmailService.java                  # Email notification service
│   └── JobScheduler.java                  # Background job coordination
├── repository/                            # Data access layer
│   ├── OrderRepository.java               # Order data access
│   ├── ReturnRepository.java              # Return data access
│   ├── StateHistoryRepository.java        # Audit trail access
│   ├── ReturnStateChangeRepository.java   # Return state tracking
│   └── JobExecutionRepository.java        # Job execution tracking
├── entity/                                # JPA entity classes
│   ├── Order.java                         # Order entity mapping
│   ├── Return.java                        # Return entity mapping
│   ├── StateHistory.java                  # State history entity
│   ├── ReturnStateChange.java             # Return state change entity
│   └── JobExecution.java                  # Job execution entity
└── dto/                                   # Data Transfer Objects and enums
    ├── CreateOrderRequest.java            # Order creation request
    ├── CreateOrderResponse.java           # Order creation response
    ├── OrderStateTransitionRequest.java   # State transition request
    ├── OrderCancellationRequest.java      # Order cancellation request
    ├── ReturnInitiationRequest.java       # Return initiation request
    ├── ReturnStateTransitionRequest.java  # Return state transition
    ├── RefundRequest.java                 # Refund processing request
    ├── RefundResponse.java                # Refund processing response
    ├── ErrorResponse.java                 # Standardized error response
    ├── OrderStatus.java                   # Order status enumeration
    └── ReturnStatus.java                  # Return status enumeration
```

#### Key Modules Explained

**Controllers Layer**
- Handles HTTP requests and responses
- Input validation and error handling
- Delegates business logic to service layer
- Returns standardized responses

**Service Layer**
- Contains core business logic
- Manages state transitions and validations
- Coordinates between different components
- Handles asynchronous job processing

**Repository Layer**
- Data access abstraction using Spring Data JPA
- Custom query methods for complex operations
- Transaction management

**Entity Layer**
- JPA entity mappings to database tables
- Relationship definitions and constraints
- Audit fields and lifecycle callbacks

**DTO Layer**
- Request/response data structures
- Input validation annotations
- Clean separation from internal entities

### Application Resources (`src/main/resources/`)

```
src/main/resources/
├── application.properties              # Default configuration
├── application-local.properties        # Local development settings
├── application-docker.properties       # Docker environment settings
├── application-test.properties         # Test environment settings
└── static/                            # Static web assets (if any)
```

#### Configuration Properties
- **Database connections** for different environments
- **Email server configuration** (Gmail SMTP)
- **Job scheduler settings** and timing
- **External service URLs** (mock payment gateway)
- **Logging configurations** and levels

## Test Code (`src/test/`)

### Test Structure (`src/test/java/com/example/orderservice/`)

```
src/test/java/com/example/orderservice/
├── controller/                            # Controller layer tests
│   ├── MockPaymentControllerTest.java
│   ├── OrderControllerIntegrationTest.java
│   ├── OrderControllerTest.java
│   └── ReturnControllerIntegrationTest.java
├── dto/                                   # DTO and validation tests
│   └── DTOTest.java
├── integration/                           # Integration test suites
│   ├── NegativeTestScenariosTest.java
│   ├── OrderIntegrationTest.java
│   └── PaymentIntegrationTest.java
├── repository/                            # Repository layer tests
│   └── OrderRepositoryTest.java
└── service/                               # Service layer tests
    ├── OrderServiceAuditLogTest.java
    ├── OrderServiceComprehensiveTest.java
    ├── OrderServiceStateTransitionTestUpdated.java
    ├── OrderServiceTest.java
    └── ReturnServiceTest.java
```

#### Test Categories
- **Unit Tests** - Isolated component testing
- **Integration Tests** - Multi-component interaction testing
- **Controller Tests** - HTTP endpoint testing with MockMvc
- **Service Tests** - Business logic validation
- **Repository Tests** - Data access testing
- **Negative Tests** - Error condition and edge case testing

### Test Resources (`src/test/resources/`)
- **application.properties** - Test-specific configuration
- **Test data fixtures** - Sample data for testing
- **Mock configurations** - Test doubles setup

## Build Artifacts (`target/`)

### Compiled Artifacts
```
target/
├── order-returns-management-1.0.0.jar  # Executable JAR file
├── order-returns-management-1.0.0.jar.original # Original JAR before repackaging
├── classes/                            # Compiled main classes
├── test-classes/                       # Compiled test classes
└── generated-sources/                  # Generated source files
```

### Test Reports and Coverage
```
target/
├── surefire-reports/                   # Unit test execution reports
│   ├── TEST-*.xml                     # Individual test results
│   └── *.txt                          # Test output logs
├── site/jacoco/                       # JaCoCo coverage reports
│   ├── index.html                     # Coverage report dashboard
│   └── **/*.html                      # Detailed coverage per class
└── jacoco.exec                        # JaCoCo execution data
```

### Maven Build Metadata
```
target/
├── maven-archiver/                     # Build metadata
│   └── pom.properties                 # Build properties
├── maven-status/                      # Compilation status
└── maven-compiler-plugin/            # Compiler plugin data
```

## Key Design Patterns and Principles

### Architecture Patterns
- **Layered Architecture** - Clear separation between presentation, business, and data layers
- **Repository Pattern** - Data access abstraction
- **Service Layer Pattern** - Business logic encapsulation
- **DTO Pattern** - Data transfer and validation

### SOLID Principles
- **Single Responsibility** - Each class has one reason to change
- **Open/Closed** - Open for extension, closed for modification
- **Liskov Substitution** - Subtypes must be substitutable for base types
- **Interface Segregation** - Clients shouldn't depend on unused interfaces
- **Dependency Inversion** - Depend on abstractions, not concretions

### Spring Boot Features
- **Auto-configuration** - Minimal configuration required
- **Dependency Injection** - Loose coupling between components
- **Aspect-Oriented Programming** - Cross-cutting concerns (transactions, logging)
- **Profile-based Configuration** - Environment-specific settings

## Development Workflow

### Local Development
1. **Database Setup** - PostgreSQL installation and configuration
2. **IDE Configuration** - Import Maven project with Spring Boot support
3. **Environment Variables** - Set email credentials and database connection
4. **Run Application** - Use Spring Boot Maven plugin or IDE
5. **API Testing** - Use Swagger UI or REST client tools

### Testing Workflow
1. **Unit Tests** - Run with `mvn test`
2. **Integration Tests** - Run with profile-specific tests
3. **Coverage Analysis** - Generate reports with `mvn jacoco:report`
4. **Quality Gates** - Ensure coverage thresholds are met

### Build and Deployment
1. **Clean Build** - `mvn clean install`
2. **Docker Build** - `docker-compose build`
3. **Environment Deployment** - Use appropriate configuration profiles
4. **Health Checks** - Verify application and database connectivity

## Extension Points

### Adding New Features
- **New Entities** - Add to entity package with proper JPA mappings
- **New Services** - Implement in service layer with proper transaction management
- **New Controllers** - Add REST endpoints with proper validation and error handling
- **New Background Jobs** - Extend job processing framework

### Configuration Extensions
- **New Environments** - Add application-{env}.properties files
- **External Services** - Add service clients with proper configuration
- **Security** - Add authentication and authorization layers
- **Monitoring** - Add actuator endpoints and metrics collection

## Best Practices Implemented

### Code Quality
- **Consistent Naming** - Clear, descriptive names for all components
- **Documentation** - Comprehensive JavaDoc and README documentation
- **Error Handling** - Standardized error responses across all endpoints
- **Validation** - Input validation at appropriate layers

### Security Considerations
- **Input Sanitization** - Proper validation and sanitization
- **SQL Injection Prevention** - Parameterized queries via JPA
- **Error Information Leakage** - Generic error messages in production
- **Dependency Management** - Regular updates and security scanning

### Performance Optimizations
- **Database Indexing** - Proper indexes for query performance
- **Connection Pooling** - Efficient database connection management
- **Async Processing** - Background jobs for time-consuming operations

### Future Considerations
- **Caching** - Introduce Redis (or similar) for frequently accessed data and read-heavy views when needed

This project structure supports scalable development, comprehensive testing, and production deployment while maintaining clean code principles and industry best practices.
