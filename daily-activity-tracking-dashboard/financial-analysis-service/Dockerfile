FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn --batch-mode package

FROM eclipse-temurin:17-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends tesseract-ocr \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /workspace/target/financial-analysis-service-1.0.0.jar /app/financial-analysis-service.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/financial-analysis-service.jar", "--server.address=0.0.0.0"]
