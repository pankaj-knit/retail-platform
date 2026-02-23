package com.retail.orderservice.kafka.event;

import java.util.List;

/**
 * Consumed from "payment-failed" topic.
 * Published by Payment Service when payment fails.
 * One event per order with embedded items list.
 */
public record PaymentFailedEvent(
        Long orderId,
        String reason,
        List<ItemDetail> items
) {
    public record ItemDetail(Long productId, int quantity) {}
}
