package com.retail.orderservice.repository;

import com.retail.orderservice.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Modifying
    @Query("UPDATE OrderItem oi SET oi.inventoryReserved = true WHERE oi.order.id = :orderId AND oi.productId = :productId")
    int markInventoryReserved(Long orderId, Long productId);
}
