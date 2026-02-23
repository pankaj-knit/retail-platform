# =============================================================================
# Multi-stage Dockerfile for Inventory Service
# Stage 1: Build with Gradle + Java 25
# Stage 2: Slim JRE runtime image
# =============================================================================

FROM eclipse-temurin:25-jdk AS builder
WORKDIR /app
COPY gradle/ gradle/
COPY gradlew build.gradle.kts settings.gradle.kts ./
RUN ./gradlew dependencies --no-daemon || true
COPY src/ src/
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN groupadd -r appuser && useradd -r -g appuser appuser
COPY --from=builder /app/build/libs/*.jar app.jar
RUN chown appuser:appuser app.jar
USER appuser
EXPOSE 8080 9090
ENTRYPOINT ["sh", "-c", "\
    if [ -f /vault/secrets/db-url ]; then export SPRING_DATASOURCE_URL=$(cat /vault/secrets/db-url); fi && \
    if [ -f /vault/secrets/db-username ]; then export SPRING_DATASOURCE_USERNAME=$(cat /vault/secrets/db-username); fi && \
    if [ -f /vault/secrets/db-password ]; then export SPRING_DATASOURCE_PASSWORD=$(cat /vault/secrets/db-password); fi && \
    exec java \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+ExitOnOutOfMemoryError \
    -jar app.jar"]
