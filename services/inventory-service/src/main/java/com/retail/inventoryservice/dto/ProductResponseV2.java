package com.retail.inventoryservice.dto;

import java.math.BigDecimal;

public record ProductResponseV2(
        Long id,
        String name,
        String description,
        BigDecimal price,
        String category,
        String imageUrl,
        int availableStock,
        StockStatus stockStatus
) {}
