package com.retail.orderservice.kafka.event;

import java.math.BigDecimal;
import java.util.List;

/**
 * Consumed from "payment-completed" topic.
 * Published by Payment Service when payment succeeds.
 * One event per order with embedded items list.
 */
public record PaymentCompletedEvent(
        Long orderId,
        BigDecimal amount,
        String transactionId,
        List<ItemDetail> items
) {
    public record ItemDetail(Long productId, int quantity) {}
}
