package com.retail.userservice.service;

import com.retail.userservice.dto.*;
import com.retail.userservice.entity.User;
import com.retail.userservice.repository.UserRepository;
import com.retail.userservice.security.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Business logic layer for user operations.
 *
 * Why a separate Service layer?
 *   Controller -> Service -> Repository
 *
 *   - Controller: Handles HTTP (request parsing, response formatting)
 *   - Service:    Business logic (validation rules, orchestration)
 *   - Repository: Data access (SQL queries)
 *
 * This separation means:
 *   - Business logic is testable without HTTP
 *   - Multiple controllers can reuse the same service
 *   - You can swap the repository (e.g., from Postgres to MongoDB)
 *     without changing business logic
 */
@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.email());
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered: " + request.email());
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phone(request.phone())
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully: id={}, email={}", user.getId(), user.getEmail());

        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());

        return new AuthResponse(
                token,
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name()
        );
    }

    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for: {}", request.email());
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("Failed login attempt for: {}", request.email());
            throw new IllegalArgumentException("Invalid email or password");
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        log.info("Login successful for: {}", request.email());

        return new AuthResponse(
                token,
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name()
        );
    }

    @Cacheable(value = "user-profile", key = "#email", sync = true)
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String email) {
        log.debug("Fetching profile for: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getRole().name(),
                user.getCreatedAt()
        );
    }

    @Cacheable(value = "user-profile-v2", key = "#email", sync = true)
    @Transactional(readOnly = true)
    public UserProfileResponseV2 getProfileV2(String email) {
        log.debug("Fetching profile (v2) for: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String fullName = user.getFirstName() + " " + user.getLastName();
        String accountAge = formatAccountAge(user.getCreatedAt());

        return new UserProfileResponseV2(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                fullName,
                user.getPhone(),
                user.getRole().name(),
                user.getCreatedAt(),
                accountAge
        );
    }

    private String formatAccountAge(LocalDateTime createdAt) {
        LocalDateTime now = LocalDateTime.now();
        long days = ChronoUnit.DAYS.between(createdAt, now);
        if (days < 1) return "Less than a day";
        if (days < 30) return days + (days == 1 ? " day" : " days");
        long months = ChronoUnit.MONTHS.between(createdAt, now);
        if (months < 12) return months + (months == 1 ? " month" : " months");
        long years = ChronoUnit.YEARS.between(createdAt, now);
        return years + (years == 1 ? " year" : " years");
    }
}
