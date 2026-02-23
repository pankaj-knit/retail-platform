package com.retail.paymentservice.kafka.event;

import java.util.List;

/**
 * Published to "payment-failed" topic when payment fails.
 * One event per order (not per item).
 * Consumed by Order Service (update status) and Inventory Service (release reserved stock per item).
 */
public record PaymentFailedEvent(
        Long orderId,
        String reason,
        List<ItemDetail> items
) {
    public record ItemDetail(Long productId, int quantity) {}
}
