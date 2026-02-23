package com.retail.userservice.dto;

import java.time.LocalDateTime;

public record UserProfileResponseV2(
        Long id,
        String email,
        String firstName,
        String lastName,
        String fullName,
        String phone,
        String role,
        LocalDateTime createdAt,
        String accountAge
) {}
