# Multi-stage build for IntentManagerMS - LLM-RAG + MoE
FROM eclipse-temurin:17-jdk-alpine as build

# Install build dependencies
RUN apk add --no-cache curl

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom.xml first for better layer caching
COPY mvnw .
COPY mvnw.cmd .
COPY .mvn .mvn
COPY pom.xml .

# Make maven wrapper executable
RUN chmod +x ./mvnw

# Download dependencies (this layer will be cached unless pom.xml changes)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

# Install runtime dependencies
RUN apk add --no-cache \
    curl \
    tzdata \
    && rm -rf /var/cache/apk/*

# Create non-root user for security
RUN addgroup -g 1001 intentmanager && \
    adduser -D -s /bin/sh -u 1001 -G intentmanager intentmanager

# Set working directory
WORKDIR /app

# Copy built JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Create directories for config and logs
RUN mkdir -p /app/config /app/logs && \
    chown -R intentmanager:intentmanager /app

# Copy configuration files
COPY --chown=intentmanager:intentmanager src/main/resources/config/ /app/config/

# Set timezone to UTC
ENV TZ=UTC

# Configure JVM options for container environment
ENV JAVA_OPTS="-Xms512m -Xmx1024m \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.profiles.active=docker \
    -Duser.timezone=UTC \
    -Dfile.encoding=UTF-8"

# Expose port
EXPOSE 8082

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8082/actuator/health || exit 1

# Switch to non-root user
USER intentmanager

# Start the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]