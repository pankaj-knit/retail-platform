package com.retail.paymentservice.kafka.event;

import java.math.BigDecimal;
import java.util.List;

/**
 * Consumed from "order-created" topic.
 * Published by Order Service when a new order is placed and stock is reserved.
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
