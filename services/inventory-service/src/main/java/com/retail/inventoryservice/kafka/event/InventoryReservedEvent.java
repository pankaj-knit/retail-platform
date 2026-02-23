package com.retail.inventoryservice.kafka.event;

/**
 * Event published to the "inventory-reserved" Kafka topic.
 * Tells other services that stock has been successfully reserved for an order.
 */
public record InventoryReservedEvent(
        Long orderId,
        Long productId,
        int quantity
) {}
