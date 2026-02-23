package com.retail.inventoryservice.dto;

public record StockCheckResponse(
        Long productId,
        int availableStock,
        boolean inStock
) {}
