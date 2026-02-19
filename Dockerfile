FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /workspace

# Copy parent POM and module POMs first (for layer caching)
COPY pom.xml .
COPY bank-masking-spring-boot-starter/pom.xml bank-masking-spring-boot-starter/
COPY bank-books-api-demo/pom.xml bank-books-api-demo/

# Resolve dependencies in a separate layer (cached unless POMs change)
RUN mvn dependency:go-offline -q

# Copy source code
COPY bank-masking-spring-boot-starter/src bank-masking-spring-boot-starter/src
COPY bank-books-api-demo/src bank-books-api-demo/src

# Build – skip tests during Docker build (tests run in CI)
RUN mvn clean package -DskipTests -q

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre-jammy

LABEL maintainer="KCB Engineering <engineering@kcbgroup.com>"
LABEL description="Bank Books API Demo with sensitive data masking"
LABEL version="1.0.0"

# Non-root user for security
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
USER appuser

WORKDIR /app

# Copy the built JAR
COPY --from=builder /workspace/bank-books-api-demo/target/bank-books-api-demo-1.0.0.jar app.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Set JVM options for containers
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseContainerSupport"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
