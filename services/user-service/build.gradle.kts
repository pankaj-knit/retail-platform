// =============================================================================
// User Service - Gradle Build Configuration (Kotlin DSL)
// =============================================================================
// This file defines:
//   - Which plugins to use (Spring Boot)
//   - Java version (25)
//   - All dependencies (libraries this service needs)
//   - Test configuration
//
// We use Gradle's native BOM (Bill of Materials) import instead of the
// io.spring.dependency-management plugin, which is the modern approach
// recommended for Gradle 9.x.
// =============================================================================

plugins {
    java
    // Spring Boot plugin: adds tasks like `bootJar` (creates executable JAR)
    // and `bootRun` (runs the app directly)
    id("org.springframework.boot") version "4.0.3"
}

group = "com.retail"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // --- Spring Boot BOM (Bill of Materials) ---
    // A BOM defines compatible versions for all Spring Boot dependencies.
    // With this, we don't need to specify versions for Spring libraries --
    // Gradle resolves them from the BOM automatically.
    implementation(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))

    // --- Spring Boot Starters ---
    // Starters are curated dependency bundles. Each one pulls in everything
    // needed for a specific feature.

    // Web: Embedded Tomcat + Spring MVC for REST APIs
    implementation("org.springframework.boot:spring-boot-starter-webmvc")

    // JPA: Hibernate ORM + Spring Data JPA for database access
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // Security: Authentication + authorization framework
    implementation("org.springframework.boot:spring-boot-starter-security")

    // Validation: Bean validation annotations (@NotBlank, @Email, @Size, etc.)
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Actuator: Production-ready endpoints (/health, /metrics, /info)
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // AspectJ: Required for transaction isolation enforcement advice
    implementation("org.springframework.boot:spring-boot-starter-aspectj")

    // --- JWT (JSON Web Tokens) ---
    // For stateless authentication.
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // --- Database ---
    runtimeOnly("org.postgresql:postgresql")

    // Flyway: Database migration tool (starter required in Spring Boot 4.x)
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")

    // --- Lombok ---
    // Compile-time annotation processor that generates boilerplate code
    // (getters, setters, constructors). "compileOnly" means Lombok is NOT
    // included in the final JAR -- it only runs during compilation.
    // "annotationProcessor" tells the compiler to run Lombok's processor.
    compileOnly("org.projectlombok:lombok:1.18.40")
    annotationProcessor("org.projectlombok:lombok:1.18.40")

    // Caching: Two-tier L1 (Caffeine) + L2 (Dragonfly via Redis protocol)
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("com.github.ben-manes.caffeine:caffeine")

    // Rate limiting for auth endpoints
    implementation("com.bucket4j:bucket4j_jdk17-core:8.14.0")

    // Observability
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.springframework.boot:spring-boot-starter-opentelemetry")
    implementation("io.opentelemetry.instrumentation:opentelemetry-logback-appender-1.0:2.25.0-alpha")

    // --- Testing ---
    testImplementation(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
