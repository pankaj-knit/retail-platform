package com.retail.inventoryservice.kafka.event;

import java.util.List;

/**
 * Event consumed from the "payment-failed" Kafka topic.
 * Published by Payment Service when payment fails.
 * One event per order with embedded items list.
 *
 * Inventory Service iterates over items to release reserved stock per product.
 */
public record PaymentFailedEvent(
        Long orderId,
        String reason,
        List<ItemDetail> items
) {
    public record ItemDetail(Long productId, int quantity) {}
}
