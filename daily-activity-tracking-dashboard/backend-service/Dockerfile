FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn --batch-mode package

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app
COPY --from=build /workspace/target/activity-dashboard-backend-1.0.0.jar /app/activity-dashboard.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/activity-dashboard.jar", "--server.address=0.0.0.0"]
