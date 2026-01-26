# Inventory Management Service - Project Structure

## Overview
The Inventory Management Service is a Spring Boot application that manages product inventory, categories, and SKUs. The service follows a layered architecture pattern with clear separation of concerns.

## Project Layout

```markdown:Project-structure.md
```
Inventory-mgmt-service/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/ecom/inventory/
│   │   │       ├── config/
│   │   │       ├── controller/
│   │   │       ├── dto/
│   │   │       ├── exception/
│   │   │       ├── mapper/
│   │   │       ├── model/
│   │   │       ├── repository/
│   │   │       ├── service/
│   │   │       └── InventoryMgmtServiceApplication.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/
│           └── com/ecom/inventory/
│               ├── service/
│               └── integration/
├── docker-compose.yml
├── Dockerfile
├── entrypoint.sh
├── pom.xml
└── README.md
```

## Directory Structure Explanation

### Source Code (`src/main/java`)

#### Core Application
- `InventoryMgmtServiceApplication.java` - Main Spring Boot application entry point

#### Package Organization

1. **Controllers** (`controller/`)
   - `CategoryController.java` - REST endpoints for category management
   - `ProductController.java` - REST endpoints for product management
   - `SKUController.java` - REST endpoints for SKU management

2. **DTOs** (`dto/`)
   - Data Transfer Objects for request/response handling
   - Separate DTOs for creation (`*CreateDTO`) and response (`*DTO`)
   - Includes DTOs for Categories, Products, and SKUs

3. **Models** (`model/`)
   - `Category.java` - Category entity
   - `Product.java` - Product entity
   - `SKU.java` - SKU entity

4. **Repositories** (`repository/`)
   - Spring Data JPA repositories
   - `CategoryRepository.java`
   - `ProductRepository.java`
   - `SKURepository.java`

5. **Services** (`service/`)
   - Business logic implementation
   - `CategoryService.java`
   - `ProductService.java`
   - `SKUService.java`

6. **Mappers** (`mapper/`)
   - Object mapping between DTOs and entities
   - `CategoryMapper.java`
   - `ProductMapper.java`
   - `SKUMapper.java`

7. **Exception Handling** (`exception/`)
   - `GlobalExceptionHandler.java` - Centralized exception handling
   - `ResourceNotFoundException.java` - Custom exception

### Test Code (`src/test/java`)

- Integration Tests
  - `InventoryMgmtIntegrationTest.java`
  - `RestPage.java` - Utility for pagination testing

- Service Tests
  - `CategoryServiceTest.java`
  - `ProductServiceTest.java`
  - `SKUServiceTest.java`

### Configuration Files

1. **Application Configuration**
   - `src/main/resources/application.properties` - Spring Boot configuration

2. **Docker Configuration**
   - `Dockerfile` - Container image definition
   - `docker-compose.yml` - Multi-container Docker setup
   - `entrypoint.sh` - Container startup script

3. **Build Configuration**
   - `pom.xml` - Maven project configuration and dependencies

## Key Features

- RESTful API endpoints for inventory management
- CRUD operations for Categories, Products, and SKUs
- Data validation and error handling
- Integration test coverage
- Docker containerization support
- Maven-based build system

## Testing Strategy

The project includes:
- Unit tests for service layer
- Integration tests for API endpoints
- Test containers for integration testing
- JaCoCo for test coverage reporting

## Build and Deployment

The service can be:
- Built using Maven
- Containerized using Docker
- Deployed using docker-compose
- Tested using the provided test scripts (`test-api.sh`)
```

This Project-structure.md file provides:
1. A visual representation of the project layout
2. Detailed explanations of each component
3. Information about the testing strategy
4. Build and deployment details

The documentation follows a clear hierarchical structure and includes all major components of the service. Would you like me to make any adjustments or add more specific details to any section? 