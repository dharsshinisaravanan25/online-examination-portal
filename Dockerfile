# Multi-stage Dockerfile for ExamNexus (Spring Boot 3 + Java 21)

# Build Stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy Maven wrapper and POM
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline -B

# Copy source code and build production JAR
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Runtime Stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create directory for H2 persistent database storage
RUN mkdir -p /app/data

# Copy built JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Dynamic port exposure (Render, Railway, etc. provide $PORT)
ENV PORT=8080
EXPOSE 8080

# Run Spring Boot application
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
