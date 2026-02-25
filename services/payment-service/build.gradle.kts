plugins {
    java
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
    maven { url = uri("https://repo.spring.io/milestone") }
}

dependencies {
    implementation(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")

    // Kafka
    implementation("org.springframework.boot:spring-boot-starter-kafka")

    // Caching: Two-tier L1 (Caffeine) + L2 (Dragonfly via Redis protocol)
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("com.github.ben-manes.caffeine:caffeine")

    // Resilience4j circuit breaker
    // TODO: Switch to resilience4j-spring-boot4 when officially released
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.3.0")
    implementation("org.springframework.boot:spring-boot-starter-aspectj")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")

    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.40")
    annotationProcessor("org.projectlombok:lombok:1.18.40")

    // Observability
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.springframework.boot:spring-boot-starter-opentelemetry")
    implementation("io.opentelemetry.instrumentation:opentelemetry-logback-appender-1.0:2.25.0-alpha")

    // Testing
    testImplementation(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
