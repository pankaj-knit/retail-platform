package com.retail.orderservice.kafka.event;

import java.math.BigDecimal;
import java.util.List;

/**
 * Published to "order-created" topic when a new order is placed.
 * Payment Service consumes this to initiate payment processing.
 */
public record OrderCreatedEvent(
        Long orderId,
        String userEmail,
        BigDecimal totalAmount,
        List<OrderItemDetail> items
) {
    public record OrderItemDetail(
            Long productId,
            int quantity,
            BigDecimal unitPrice
    ) {}
}
