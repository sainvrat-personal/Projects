# Multi-stage build for Spring Boot application

# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy the Maven POM file and download dependencies
COPY pom.xml .
# This step creates a layer with just the dependencies to improve build caching
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jdk
WORKDIR /app

# Add maintainer info
LABEL maintainer="Development Team <dev@example.com>"

# Add application user
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

# Create invoices directory with appropriate permissions
RUN mkdir -p /app/invoices && chown appuser:appgroup /app/invoices

# Set the user for subsequent operations
USER appuser

# Copy the built artifact from the build stage
COPY --from=build /app/target/order-returns-management-*.jar app.jar

# Environment variables with defaults
ENV SERVER_PORT=8080 \
    SPRING_PROFILES_ACTIVE=docker

# Expose the application port
EXPOSE 8080

# Health check to verify application is running
HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
