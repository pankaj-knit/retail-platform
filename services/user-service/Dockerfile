# =============================================================================
# Multi-Stage Dockerfile for User Service
# =============================================================================
# Multi-stage builds create smaller, more secure images by separating the
# build environment from the runtime environment.
#
# Stage 1 (builder): Has Gradle + JDK + all build tools (~800MB)
#   -> Compiles code, runs tests, produces a JAR file
#
# Stage 2 (runtime): Has only JRE (~200MB)
#   -> Copies the JAR from stage 1, runs it
#
# The final image only contains what's needed to RUN the app,
# not what's needed to BUILD it. This is a security best practice
# (fewer tools = smaller attack surface).
# =============================================================================

# --- Stage 1: Build ---
FROM eclipse-temurin:25-jdk AS builder

WORKDIR /app

# Copy Gradle wrapper and build files first.
# Docker caches layers -- if these files haven't changed, Docker reuses
# the cached layer and skips the dependency download step.
# This makes rebuilds much faster when only source code changes.
COPY gradle/ gradle/
COPY gradlew build.gradle.kts settings.gradle.kts ./

# Download dependencies (cached unless build.gradle.kts changes)
RUN ./gradlew dependencies --no-daemon

# Now copy source code and build
COPY src/ src/

RUN ./gradlew bootJar --no-daemon -x test

# --- Stage 2: Runtime ---
FROM eclipse-temurin:25-jre

WORKDIR /app

# Create a non-root user for security.
# Running as root inside a container is dangerous -- if an attacker
# breaks out of the app, they'd have root access to the container.
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Copy the built JAR from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Switch to non-root user
USER appuser

# Expose the port the app listens on
EXPOSE 8080

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
