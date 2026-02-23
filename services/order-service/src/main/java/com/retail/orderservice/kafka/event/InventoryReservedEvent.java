package com.retail.orderservice.kafka.event;

/**
 * Consumed from "inventory-reserved" topic.
 * Published by Inventory Service when stock is successfully reserved.
 */
public record InventoryReservedEvent(
        Long orderId,
        Long productId,
        int quantity
) {}
