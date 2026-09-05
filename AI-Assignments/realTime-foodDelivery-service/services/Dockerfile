FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
ARG SERVICE_MODULE=backend-service
COPY pom.xml .
COPY platform-lib/pom.xml platform-lib/pom.xml
COPY entities-service/pom.xml entities-service/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY refund-service/pom.xml refund-service/pom.xml
COPY analytics-service/pom.xml analytics-service/pom.xml
COPY backend-service/pom.xml backend-service/pom.xml
COPY platform-lib/src platform-lib/src
COPY ${SERVICE_MODULE}/src ${SERVICE_MODULE}/src
RUN mvn -q -pl ${SERVICE_MODULE} -am -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
ARG SERVICE_MODULE=backend-service
ARG SERVICE_PORT=8080
COPY --from=build /app/${SERVICE_MODULE}/target/${SERVICE_MODULE}-*.jar app.jar
EXPOSE ${SERVICE_PORT}
ENTRYPOINT ["java", "-jar", "app.jar"]
