package com.retail.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Entry point for the User Service.
 *
 * @SpringBootApplication is a convenience annotation that combines:
 *   - @Configuration:       This class can define @Bean methods
 *   - @EnableAutoConfiguration: Spring Boot auto-configures based on classpath
 *   - @ComponentScan:       Scans this package and sub-packages for @Component, @Service, etc.
 *
 * When this runs, Spring Boot:
 *   1. Starts embedded Tomcat on port 8080
 *   2. Connects to Postgres using application.yaml config
 *   3. Runs Flyway migrations to create/update tables
 *   4. Registers all REST controllers, services, and repositories
 *   5. Configures Spring Security with our JWT filter
 */
@SpringBootApplication
@EnableCaching
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
