# Order & Returns Management System

## Overview

The Order & Returns Management System is a comprehensive Spring Boot application designed to handle the complete lifecycle of e-commerce orders and returns. It provides a robust, scalable solution for managing order states, processing returns, handling refunds, and maintaining comprehensive audit trails.

## Key Features

### Core Functionality
- **Order Management**: Complete order lifecycle from creation to delivery
- **Return Processing**: Eligibility-based return initiation and state management
- **State Machines**: Well-defined state transitions with validation rules
- **Audit Trail**: Comprehensive history tracking for all state changes
- **Background Jobs**: Asynchronous processing for invoices and refunds

### Technical Features
- **RESTful APIs**: Complete set of endpoints with consistent error handling
- **Database Integration**: PostgreSQL with proper schema design and migrations
- **Email Integration**: Gmail SMTP integration for notifications
- **Mock Payment**: Simulated payment gateway for refund processing
- **API Documentation**: Swagger/OpenAPI integration with interactive documentation
- **Testing**: Comprehensive unit and integration test coverage
- **Docker Support**: Complete containerization with Docker Compose

### Quality Assurance
- **Input Validation**: Robust validation at all API layers
- **Error Handling**: Standardized error responses across all endpoints
- **Retry Logic**: Intelligent retry mechanisms for background jobs
- **Monitoring**: Job execution tracking and manual intervention capabilities

## System Architecture

### Order State Flow
```
PENDING_PAYMENT → PAID → PROCESSING_IN_WAREHOUSE → SHIPPED → DELIVERED
                ↓
            CANCELLED (only from PENDING_PAYMENT or PAID)
```

### Return State Flow
```
REQUESTED → APPROVED → IN_TRANSIT → RECEIVED → COMPLETED
         ↓
       REJECTED
```

### Background Job Processing
- **Invoice Generation**: Automatically triggered when orders are SHIPPED
- **Refund Processing**: Automatically triggered when returns are COMPLETED
- **Retry Mechanism**: Failed jobs are automatically retried with exponential backoff
- **Manual Intervention**: Failed jobs can be manually retried via API

## Prerequisites

Before setting up the application, ensure you have:

- **Java 17** or higher
- **Maven 3.6+** for build management
- **PostgreSQL 12+** (or use Docker Compose setup)
- **Docker & Docker Compose** (optional, for containerized setup)
- **Gmail Account** (for email functionality)

## Quick Start

### Option 1: Docker Compose (Recommended)

This is the easiest way to get the application running with all dependencies.

1. **Clone the repository:**
   ```bash
   git clone <repository-url>
   cd order-returns-management
   ```

2. **Set up environment variables:**
   ```bash
   export EMAIL_USERNAME=your-email@gmail.com
   export EMAIL_PASSWORD=your-app-password
   ```

3. **Start the application:**
   ```bash
   EMAIL_USERNAME=sainvrat@gmail.com EMAIL_PASSWORD=qjtxzvcsclhioigp docker-compose up  --build
   ```
   To stop the containers:
   ```bash
   docker-compose down
   ```
   
   To stop and remove volumes:
   ```bash
   docker-compose down -v
   ```

4. **Access the application:**
   - Application: http://localhost:8080
   - Swagger UI: http://localhost:8080/swagger-ui.html
   - Database: localhost:5432 (postgres/postgres)

### Option 2: Local Development Setup

1. **Set up PostgreSQL:**
   ```bash
   # Install PostgreSQL and create database
   psql -U postgres -c "CREATE DATABASE orders_db;"
   ```

2. **Build the application:**
   ```bash
   mvn clean install
   ```

3. **Run tests:**
   ```bash
   mvn test
   ```

4. **Start the application:**
   ```bash
   java -jar target/order-returns-management-1.0.0.jar --spring.profiles.active=local
   ```

## Database Setup

### Automatic Setup (Docker Compose)
When using Docker Compose, the database is automatically:
- Created with name `orders_db`
- Initialized with schema from `db/migration/V1__init_schema.sql`
- Configured with user `postgres` and password `postgres`

### Manual Setup (Local Development)
1. **Install PostgreSQL** (version 12 or higher)
2. **Create database:**
   ```bash
   createdb -U postgres orders_db
   ```
3. **The application will automatically run migrations on startup**

### Database Schema
The application uses the following key tables:
- `orders` - Order data and current state
- `returns` - Return data and current state  
- `state_history` - Audit trail of all state transitions
- `return_state_history` - Return-specific state change tracking
- `job_execution` - Background job tracking and status

For detailed schema information, see [db/schema-diagram.md](db/schema-diagram.md)

## Email Configuration

The application integrates with Gmail SMTP for sending notifications and invoices.

### Setting up Gmail App Password

1. **Enable 2-Step Verification:**
   - Go to your Google Account → Security
   - Turn on 2-Step Verification

2. **Create App Password:**
   - Go to Security → App Passwords
   - Select "Mail" and your device
   - Use the generated 16-character password

3. **Configure the application:**
   ```bash
   # Environment variables
   export EMAIL_USERNAME=your-email@gmail.com
   export EMAIL_PASSWORD=your-16-char-app-password
   export EMAIL_ENABLED=true
   ```

   Or in `application-local.properties`:
   ```properties
   spring.mail.username=your-email@gmail.com
   spring.mail.password=your-16-char-app-password
   email.sending.enabled=true
   ```

### Email Features
- **Order Confirmation**: Sent when order is created
- **Shipping Notification**: Sent when order is shipped (with PDF invoice)
- **Return Confirmation**: Sent when return is initiated
- **Refund Notification**: Sent when refund is processed

## Running Background Workers

The application includes built-in background job processing that runs automatically:

### Job Types
1. **Invoice Generation Job**
   - **Trigger**: Order status changes to SHIPPED
   - **Function**: Creates PDF invoice and sends via email
   - **Retry**: Up to 3 attempts with exponential backoff

2. **Refund Processing Job**
   - **Trigger**: Return status changes to COMPLETED
   - **Function**: Processes refund via mock payment gateway
   - **Retry**: Up to 3 attempts with exponential backoff

### Job Monitoring
Monitor background jobs using the Jobs API:
```bash
# Get all jobs
curl http://localhost:8080/jobs

# Get specific job
curl http://localhost:8080/jobs/{jobId}

# Get jobs by status
curl http://localhost:8080/jobs/status/FAILED

# Retry a job (including FAILED or manually re-running COMPLETED jobs)
curl -X POST http://localhost:8080/jobs/{jobId}/retry
```

### Job Configuration
Configure job processing in `application.properties`:
```properties
# Enable/disable job scheduler
scheduler.enabled=true

# Job processing intervals (milliseconds)
scheduler.process-jobs-rate=30000          # Process pending jobs every 30 seconds
scheduler.process-retry-jobs-rate=60000    # Process retry jobs every 60 seconds

# Async task executor settings
spring.task.execution.pool.core-size=5
spring.task.execution.pool.max-size=10
spring.task.execution.pool.queue-capacity=25
```

## API Documentation

### Interactive Documentation
- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON**: [http://localhost:8080/api-docs](http://localhost:8080/api-docs)
- **API Specification**: [API-SPECIFICATION.yml](API-SPECIFICATION.yml)
- **Endpoint Details**: [apis.md](apis.md)

### Core API Endpoints

#### Order Management
- `POST /orders/order` - Create a new order
- `PATCH /orders/{orderId}/state` - Update order state
- `PATCH /orders/{orderId}/cancel` - Cancel an order
- `GET /orders/{orderId}/state-history` - Get order state history

#### Return Management
- `POST /returns/{orderId}` - Initiate a return for an order
- `PATCH /returns/{returnId}/state` - Update return state
- `GET /returns/{returnId}/state-history` - Get return state history

#### Job Management
- `GET /jobs` - Get all background jobs
- `GET /jobs/{jobId}` - Get specific job details
- `GET /jobs/status/{status}` - Get jobs by status
- `POST /jobs/{jobId}/retry` - Retry a failed (or manually re-run a completed) job

#### Mock Payment
- `POST /mock-payment/refund` - Process refund (mock endpoint)

### Order States
- `PENDING_PAYMENT` - Initial state when order is created
- `PAID` - Payment has been received
- `PROCESSING_IN_WAREHOUSE` - Order is being prepared for shipping
- `SHIPPED` - Order has been shipped (triggers invoice generation)
- `DELIVERED` - Order has been delivered
- `CANCELLED` - Order has been cancelled

### Return States
- `REQUESTED` - Initial state when return is requested
- `APPROVED` - Return has been approved
- `REJECTED` - Return has been rejected
- `IN_TRANSIT` - Return items are in transit
- `RECEIVED` - Return items have been received
- `COMPLETED` - Return and refund are complete (triggers refund processing)

### Example API Usage

#### Create an Order
```bash
curl -X POST http://localhost:8080/orders/order \
  -H "Content-Type: application/json" \
  -d '{
  "customerId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "item": {
    "productId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "quantity": 1,
    "price": 99.99
  },
  "shippingAddress": "hisar",
  "email": "sainvratmundara@gmail.com",
  "transactionId": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
}'
```

#### Update Order State
```bash
curl -X PATCH http://localhost:8080/orders/{orderId}/state \
  -H "Content-Type: application/json" \
  -d '{
    "newState": "PAID",
    "changedBy": "550e8400-e29b-41d4-a716-446655440003"
  }'
```

#### Initiate Return
```bash
curl -X POST http://localhost:8080/returns/{orderId} \
  -H "Content-Type: application/json" \
  -d '{
    "changedBy": "550e8400-e29b-41d4-a716-446655440003"
  }'
```

### Background Jobs End-to-End (Example cURL Flow)

The following example shows how to create an order, drive it through the states that trigger background jobs, and then inspect the resulting job records.

```bash
# 1) Create an order (PENDING_PAYMENT)
ORDER_JSON=$(curl -s -X POST http://localhost:8080/orders/order \
  -H "Content-Type: application/json" \
  -d '{
  "customerId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "item": {
    "productId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "quantity": 1,
    "price": 99.99
  },
  "shippingAddress": "hisar",
  "email": "sainvratmundara@gmail.com",
  "transactionId": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
}')

ORDER_ID=$(echo "$ORDER_JSON" | jq -r '.id')
echo "Created order: $ORDER_ID"

# 2) Move order to PAID, then to SHIPPED (SHIPPED triggers invoice job creation)
curl -X PATCH http://localhost:8080/orders/$ORDER_ID/state \
  -H "Content-Type: application/json" \
  -d '{
    "newState": "PAID",
    "changedBy": "550e8400-e29b-41d4-a716-446655440003"
  }'

curl -X PATCH http://localhost:8080/orders/$ORDER_ID/state \
  -H "Content-Type: application/json" \
  -d '{
    "newState": "SHIPPED",
    "changedBy": "550e8400-e29b-41d4-a716-446655440003"
  }'

# 3) (Optional) Manually trigger job processing once, if you do not want to wait for the scheduler tick
curl -X POST http://localhost:8080/jobs/process

# 4) Inspect jobs for this order (invoice generation jobs use the orderId as entityId)
curl http://localhost:8080/jobs/entity/$ORDER_ID | jq .

# 5) Create a return for the order (driving it to COMPLETED will trigger a refund job)
RETURN_JSON=$(curl -s -X POST http://localhost:8080/returns/$ORDER_ID \
  -H "Content-Type: application/json" \
  -d '{
    "changedBy": "550e8400-e29b-41d4-a716-446655440003"
  }')

RETURN_ID=$(echo "$RETURN_JSON" | jq -r '.id')
echo "Created return: $RETURN_ID"

# 6) Advance the return through APPROVED → IN_TRANSIT → RECEIVED → COMPLETED
for STATE in APPROVED IN_TRANSIT RECEIVED COMPLETED; do
  curl -X PATCH http://localhost:8080/returns/$RETURN_ID/state \
    -H "Content-Type: application/json" \
    -d "{
      \"newState\": \"$STATE\",
      \"changedBy\": \"550e8400-e29b-41d4-a716-446655440003\"
    }"
done

# 7) Trigger job processing again so the refund job is picked up
curl -X POST http://localhost:8080/jobs/process

# 8) Inspect jobs for this return (refund jobs use the returnId as entityId)
curl http://localhost:8080/jobs/entity/$RETURN_ID | jq .
```

### Error Handling

All API endpoints return standardized error responses:

```json
{
  "status": 400,
  "error": "Bad Request",
  "code": "INVALID_REQUEST",
  "message": "Quantity must be greater than zero.",
  "path": "/orders/order",
  "timestamp": "2026-01-26T08:02:09.191229713Z",
  "details": {
    "reason": "The request contains invalid or malformed data"
  },
  "validationErrors": null
}
```

**Common HTTP Status Codes:**
- `200 OK` - Successful GET, PATCH requests
- `201 Created` - Successful POST requests
- `400 Bad Request` - Invalid request data
- `404 Not Found` - Resource not found
- `422 Unprocessable Entity` - Invalid state transitions or business logic violations
- `500 Internal Server Error` - Server errors

## Testing

### Running Tests

The application includes comprehensive test coverage across all layers:

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=OrderServiceTest

# Run integration tests
mvn test -Dtest="*IntegrationTest"

# Generate coverage report
mvn jacoco:report
```

### Test Coverage
- **Overall Coverage**: ~90% line coverage, measured via JaCoCo
- **Unit Tests**: DTOs, services, background jobs, and exception handling
- **Integration Tests**: Controllers, repositories, and end-to-end flows (orders, returns, refunds)
- **Negative & Edge Cases**: Invalid inputs, state-machine violations, concurrency and retry behavior

**Coverage Report**: Available at `target/site/jacoco/index.html` after running `mvn jacoco:report`

### Test Strategy
For detailed information about our testing approach, see [TESTING_STRATEGY.md](TESTING_STRATEGY.md)

## Configuration

### Application Profiles

The application supports multiple profiles for different environments:

- **`local`** - Local development with PostgreSQL
- **`docker`** - Docker Compose environment
- **`test`** - Test environment with H2 database

### Key Configuration Properties

```properties
# Server Configuration
server.port=8080

# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/orders_db
spring.datasource.username=postgres
spring.datasource.password=postgres

# Email Configuration
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${EMAIL_USERNAME}
spring.mail.password=${EMAIL_PASSWORD}
email.sending.enabled=true

# Job Scheduler Configuration
scheduler.enabled=true
scheduler.process-jobs-rate=30000
scheduler.process-retry-jobs-rate=60000

# External Service URLs
mock.payment.url=http://localhost:8080/mock-payment/refund
```

## Monitoring and Observability

### Application Health
- **Health Check**: http://localhost:8080/actuator/health (if actuator is enabled)
- **Application Logs**: Comprehensive logging at appropriate levels
- **Database Connection**: Connection pool monitoring

### Job Monitoring
Monitor background job execution through the Jobs API:
- View all jobs and their status
- Track retry attempts and failure reasons
- Manually trigger retries for failed jobs
- Monitor job execution times and patterns

### Metrics Collection
The application logs key metrics for:
- Order creation and state transition rates
- Return processing times
- Background job success/failure rates
- Email delivery status

## Production Deployment Considerations

### Security
- **Authentication**: Add JWT or OAuth2 authentication
- **Authorization**: Implement role-based access control
- **Input Validation**: Enhanced validation for production data
- **Rate Limiting**: Implement API rate limiting
- **HTTPS**: Use SSL/TLS for all communications, would have to be done via infra (reverse proxy, certs).

### Performance
- **Database**: Configure connection pooling and query optimization
- **Load Balancing**: Use multiple application instances while deploying

### Future Considerations
- **Caching**: Introduce Redis (or similar) for frequently accessed data and read-heavy views once production traffic patterns are understood

### Monitoring
- **APM**: Application Performance Monitoring (e.g., New Relic, DataDog)
- **Logging**: Centralized logging with ELK stack
- **Alerting**: Set up alerts for critical failures
- **Health Checks**: Comprehensive health monitoring

### Data Management
- **Backups**: Regular database backups
- **Data Retention**: Policies for audit trail data
- **Archiving**: Move old data to archive storage
- **Compliance**: GDPR/data protection compliance
Note : These are operational policies; there’s nothing in the codebase implementing them explicitly.

## Accessing the database with pgAdmin

When running via Docker Compose, a pgAdmin instance is started alongside Postgres to make it easy to inspect tables and data.

1. **Open pgAdmin UI**
   - Navigate to `http://localhost:5050` in your browser.
   - Log in with:
     - **Email**: `admin@example.com` (or value of `PGADMIN_EMAIL`)
     - **Password**: `admin` (or value of `PGADMIN_PASSWORD`)

2. **Register a new server in pgAdmin**
   - Right-click `Servers` → `Register` → `Server...`
   - **General** tab:
     - **Name**: `order-returns-db` (any name is fine)
   - **Connection** tab:
     - **Host name/address**: `order-returns-db`
     - **Port**: `5432`
     - **Maintenance database**: `orders_db`
     - **Username**: `postgres`
     - **Password**: `postgres`
   - Click **Save**.

3. **Browse tables and data**
   - Expand: `Servers → order-returns-db → Databases → orders_db → Schemas → public → Tables`.
   - Right-click a table (for example `orders`, `returns`, `state_history`, `return_state_history`, `job_execution`) and choose `View/Edit Data → All Rows` to inspect records.

## Troubleshooting

### Common Issues

1. **Database Connection Failed**
   ```bash
   # Check PostgreSQL is running
   pg_isready -h localhost -p 5432
   
   # Verify database exists
   psql -U postgres -l | grep orders_db
   ```

2. **Email Not Sending**
   ```bash
   # Verify Gmail app password is correct
   # Check application logs for SMTP errors
   # Ensure 2FA is enabled on Gmail account
   ```

3. **Background Jobs Not Processing**
   ```bash
   # Check scheduler is enabled
   # Verify database job_execution table
   # Check application logs for job processor errors
   ```

4. **Docker Compose Issues**
   ```bash
   # Clean rebuild
   docker-compose down -v
   docker-compose up --build
   
   # Check container logs
   docker-compose logs app
   docker-compose logs db
   ```

### Logging
Application logs provide detailed information about:
- API request/response cycles
- State transition attempts and validations
- Background job execution
- Email sending status
- Database operations

Log levels can be configured in `application.properties`:
```properties
logging.level.com.example.orderservice=DEBUG
logging.level.org.springframework=INFO
```

## Contributing

### Development Setup
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run tests: `mvn test`
5. Check code coverage: `mvn jacoco:report`
6. Submit a pull request

### Code Standards
- Follow Spring Boot best practices
- Maintain test coverage above 80%
- Use meaningful commit messages
- Update documentation for API changes

## Documentation

- **[PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md)** - Project structure and module purposes
- **[WORKFLOW_DESIGN.md](WORKFLOW_DESIGN.md)** - State machine diagrams and workflow design
- **[APIs Documentation](apis.md)** - Detailed API endpoint documentation
- **[API Specification](API-SPECIFICATION.yml)** - OpenAPI 3.0 specification
- **[Testing Strategy](TESTING_STRATEGY.md)** - Comprehensive testing approach
- **[Email Configuration](EMAIL_CONFIG.md)** - Email setup and configuration

## Support

For questions, issues, or contributions:
- **Issues**: Create a GitHub issue
- **Discussions**: Use GitHub discussions for questions
- **Email**: Contact the development team

---

**Version**: 1.0.0  
**Last Updated**: September 2025  
**License**: MIT

4. Verify it’s running
Once logs show the app started and the healthcheck passes:
App health (if actuator enabled):
http://localhost:8080/actuator/health

Swagger UI:
http://localhost:8080/swagger-ui.html

App base URL:
http://localhost:8080

Postgres (from host, if you want):
Host: localhost
Port: 5432
DB: orders_db
User/Password: postgres / postgres

pgAdmin:
http://localhost:5050 (credentials from docker-compose.yml)
Default Email : admin@example.com
Default Password : admin
