package com.retail.inventoryservice.kafka.event;

import java.math.BigDecimal;
import java.util.List;

/**
 * Event consumed from the "payment-completed" Kafka topic.
 * Published by Payment Service when payment succeeds.
 * One event per order with embedded items list.
 *
 * Inventory Service iterates over items to confirm stock deduction per product.
 */
public record PaymentCompletedEvent(
        Long orderId,
        BigDecimal amount,
        String transactionId,
        List<ItemDetail> items
) {
    public record ItemDetail(Long productId, int quantity) {}
}
