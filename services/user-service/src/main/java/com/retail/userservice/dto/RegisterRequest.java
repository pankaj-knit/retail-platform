package com.retail.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for user registration requests.
 *
 * DTOs (Data Transfer Objects) are separate from Entities because:
 *   1. Security: We don't expose internal fields (id, passwordHash, timestamps)
 *   2. Validation: DTOs carry validation annotations for input checking
 *   3. Decoupling: API contract is independent of database schema
 *
 * Java Records (introduced in Java 16) are perfect for DTOs:
 *   - Immutable by default
 *   - Auto-generates constructor, getters, equals, hashCode, toString
 *   - Much less boilerplate than a regular class
 */
public record RegisterRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Must be a valid email address")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        String phone
) {}
