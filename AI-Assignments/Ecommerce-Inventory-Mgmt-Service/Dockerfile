# Build stage
FROM maven:3.8.4-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean install -DskipTests

# Package stage
# Use a more standard and full-featured base image
FROM openjdk:17-jdk-slim

# Set the working directory in the container
WORKDIR /app

# Copy the Maven build artifact (the JAR file) into the container
COPY target/inventory-mgmt-service-0.0.1-SNAPSHOT.jar app.jar

# Set the command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"] 