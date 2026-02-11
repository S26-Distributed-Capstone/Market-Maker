# Build stage
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Add a non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder /app/target/*.jar app.jar

# Change ownership to non-root user
RUN chown -R appuser:appgroup /app
USER appuser

# Default port (can be overridden)
EXPOSE 8080

# Spring profile is passed as environment variable
ENV SPRING_PROFILES_ACTIVE=default

ENTRYPOINT ["java", "-jar", "app.jar"]

# to build:
# docker build -t market-maker:latest .

# Exchange Service
# docker run -e SPRING_PROFILES_ACTIVE=exchange -p 8081:8080 market-maker:latest

# Trading State Service
# docker run -e SPRING_PROFILES_ACTIVE=trading-state -p 8082:8080 market-maker:latest

# Exposure Reservation Service
# docker run -e SPRING_PROFILES_ACTIVE=exposure-reservation -p 8083:8080 market-maker:latest

# Market-Maker Node
# docker run -e SPRING_PROFILES_ACTIVE=market-maker-node -p 8084:8080 market-maker:latest

# External Order Publisher
# docker run -e SPRING_PROFILES_ACTIVE=external-publisher -p 8085:8080 market-maker:latest

# Position Display UI
# docker run -e SPRING_PROFILES_ACTIVE=position-ui -p 8086:8080 market-maker:latest
