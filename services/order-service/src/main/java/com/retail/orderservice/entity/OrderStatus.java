package com.retail.orderservice.entity;

public enum OrderStatus {
    PENDING,
    INVENTORY_RESERVED,
    PAYMENT_PENDING,
    PAYMENT_COMPLETED,
    PAYMENT_FAILED,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
