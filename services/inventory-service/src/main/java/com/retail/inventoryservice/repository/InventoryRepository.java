package com.retail.inventoryservice.repository;

import com.retail.inventoryservice.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductId(Long productId);

    List<Inventory> findByProductIdIn(Collection<Long> productIds);

    /**
     * Atomically reserve stock for a product.
     *
     * This uses a conditional UPDATE: it only succeeds if there's enough
     * available stock (quantity - reserved >= requested amount).
     *
     * Why a raw SQL query instead of findByProductId + save?
     *   Race condition: Two concurrent orders could both read quantity=10,
     *   both think there's enough stock, and both reserve -- overselling.
     *
     *   The WHERE clause makes this atomic at the database level:
     *   only ONE of the concurrent updates will succeed because Postgres
     *   uses row-level locking during UPDATE.
     *
     * @return number of rows updated (1 = success, 0 = insufficient stock)
     */
    @Modifying
    @Query("UPDATE Inventory i SET i.reserved = i.reserved + :amount " +
           "WHERE i.productId = :productId AND (i.quantity - i.reserved) >= :amount")
    int reserveStock(@Param("productId") Long productId, @Param("amount") int amount);

    /**
     * Release reserved stock (when payment fails or order is cancelled).
     */
    @Modifying
    @Query("UPDATE Inventory i SET i.reserved = i.reserved - :amount " +
           "WHERE i.productId = :productId AND i.reserved >= :amount")
    int releaseStock(@Param("productId") Long productId, @Param("amount") int amount);

    /**
     * Confirm stock deduction (when payment succeeds).
     * Decrements both quantity and reserved.
     */
    @Modifying
    @Query("UPDATE Inventory i SET i.quantity = i.quantity - :amount, " +
           "i.reserved = i.reserved - :amount " +
           "WHERE i.productId = :productId AND i.reserved >= :amount")
    int confirmStockDeduction(@Param("productId") Long productId, @Param("amount") int amount);
}
