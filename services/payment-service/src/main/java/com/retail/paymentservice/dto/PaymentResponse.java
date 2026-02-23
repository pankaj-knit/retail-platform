package com.retail.paymentservice.dto;

import com.retail.paymentservice.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        Long orderId,
        String userEmail,
        BigDecimal amount,
        PaymentStatus status,
        String transactionId,
        String failureReason,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {}
