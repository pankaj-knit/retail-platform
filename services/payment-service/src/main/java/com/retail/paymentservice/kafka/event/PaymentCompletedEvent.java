package com.retail.paymentservice.kafka.event;

import java.math.BigDecimal;
import java.util.List;

/**
 * Published to "payment-completed" topic when payment succeeds.
 * One event per order (not per item).
 * Consumed by Order Service (update status) and Inventory Service (confirm deduction per item).
 */
public record PaymentCompletedEvent(
        Long orderId,
        BigDecimal amount,
        String transactionId,
        List<ItemDetail> items
) {
    public record ItemDetail(Long productId, int quantity) {}
}
