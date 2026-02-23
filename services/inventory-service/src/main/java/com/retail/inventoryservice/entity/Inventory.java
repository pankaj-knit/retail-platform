package com.retail.inventoryservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Inventory (stock level) entity.
 *
 * Tracks how many units of a product are available and how many are reserved.
 * Available stock = quantity - reserved.
 *
 * The "reserved" field is key to preventing overselling:
 *   1. Customer places order → reserved += orderQuantity
 *   2. Payment succeeds     → quantity -= orderQuantity, reserved -= orderQuantity
 *   3. Payment fails        → reserved -= orderQuantity (release the reservation)
 *
 * This is the "reservation pattern" -- common in e-commerce to handle the gap
 * between order placement and payment confirmation.
 */
@Entity
@Table(name = "inventory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false, unique = true)
    private Long productId;

    @Builder.Default
    @Column(nullable = false)
    private int quantity = 0;

    @Builder.Default
    @Column(nullable = false)
    private int reserved = 0;

    @Version
    private Long version;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public int getAvailableStock() {
        return quantity - reserved;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Inventory inv)) return false;
        return id != null && id.equals(inv.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
