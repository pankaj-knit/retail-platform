package com.retail.orderservice.service;

import com.retail.inventoryservice.proto.ReserveStockResponse;
import com.retail.orderservice.dto.*;
import com.retail.orderservice.entity.Order;
import com.retail.orderservice.entity.OrderItem;
import com.retail.orderservice.entity.OrderStatus;
import com.retail.orderservice.grpc.InventoryGrpcClient;
import com.retail.orderservice.kafka.OrderEventProducer;
import com.retail.orderservice.kafka.event.OrderCreatedEvent;
import com.retail.orderservice.repository.OrderItemRepository;
import com.retail.orderservice.repository.OrderRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final InventoryGrpcClient inventoryGrpcClient;
    private final OrderEventProducer orderEventProducer;

    private final Counter ordersCreatedCounter;
    private final Counter ordersCancelledCounter;
    private final Counter ordersFailedCounter;
    private final Counter orderItemsCounter;
    private final Counter orderRevenueCounter;
    private final Counter sagaCompletedCounter;
    private final Counter sagaFailedCounter;
    private final Timer orderCreationTimer;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        InventoryGrpcClient inventoryGrpcClient,
                        OrderEventProducer orderEventProducer,
                        MeterRegistry meterRegistry) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.inventoryGrpcClient = inventoryGrpcClient;
        this.orderEventProducer = orderEventProducer;

        this.ordersCreatedCounter = Counter.builder("orders.created.total")
                .description("Total orders successfully created")
                .register(meterRegistry);
        this.ordersCancelledCounter = Counter.builder("orders.cancelled.total")
                .description("Total orders cancelled")
                .register(meterRegistry);
        this.ordersFailedCounter = Counter.builder("orders.failed.total")
                .description("Total orders that failed during creation (e.g. stock)")
                .register(meterRegistry);
        this.orderItemsCounter = Counter.builder("orders.items.total")
                .description("Total items across all orders")
                .register(meterRegistry);
        this.orderRevenueCounter = Counter.builder("orders.revenue.total")
                .description("Cumulative order revenue")
                .register(meterRegistry);
        this.sagaCompletedCounter = Counter.builder("saga.completed.total")
                .description("Saga completed (payment success)")
                .register(meterRegistry);
        this.sagaFailedCounter = Counter.builder("saga.failed.total")
                .description("Saga failed (payment failure)")
                .register(meterRegistry);
        this.orderCreationTimer = Timer.builder("orders.creation.duration")
                .description("End-to-end order creation time (DB + gRPC + Kafka publish)")
                .register(meterRegistry);
    }

    @Transactional
    public OrderResponse createOrder(String userEmail, CreateOrderRequest request) {
        return orderCreationTimer.record(() -> {
            log.info("Creating order for user: {}, items: {}", userEmail, request.items().size());

            BigDecimal totalAmount = BigDecimal.ZERO;
            Order order = Order.builder()
                    .userEmail(userEmail)
                    .status(OrderStatus.PENDING)
                    .shippingAddress(request.shippingAddress())
                    .totalAmount(BigDecimal.ZERO)
                    .build();

            for (OrderItemRequest itemReq : request.items()) {
                BigDecimal subtotal = itemReq.unitPrice().multiply(BigDecimal.valueOf(itemReq.quantity()));
                totalAmount = totalAmount.add(subtotal);

                OrderItem item = OrderItem.builder()
                        .productId(itemReq.productId())
                        .productName(itemReq.productName())
                        .quantity(itemReq.quantity())
                        .unitPrice(itemReq.unitPrice())
                        .subtotal(subtotal)
                        .build();
                order.addItem(item);
            }

            order.setTotalAmount(totalAmount);
            Order savedOrder = orderRepository.save(order);
            log.info("Order persisted: orderId={}, total={}", savedOrder.getId(), totalAmount);

            for (OrderItem item : savedOrder.getItems()) {
                ReserveStockResponse response = inventoryGrpcClient.reserveStock(
                        savedOrder.getId(), item.getProductId(), item.getQuantity());

                if (!response.getSuccess()) {
                    log.warn("Stock reservation failed for orderId={}, productId={}: {}",
                            savedOrder.getId(), item.getProductId(), response.getMessage());
                    savedOrder.setStatus(OrderStatus.CANCELLED);
                    orderRepository.save(savedOrder);
                    ordersFailedCounter.increment();
                    throw new IllegalStateException("Insufficient stock for product: " + item.getProductName());
                }

                item.setInventoryReserved(true);
            }

            savedOrder.setStatus(OrderStatus.INVENTORY_RESERVED);
            orderRepository.save(savedOrder);

            OrderCreatedEvent event = new OrderCreatedEvent(
                    savedOrder.getId(),
                    savedOrder.getUserEmail(),
                    savedOrder.getTotalAmount(),
                    savedOrder.getItems().stream()
                            .map(i -> new OrderCreatedEvent.OrderItemDetail(
                                    i.getProductId(), i.getQuantity(), i.getUnitPrice()))
                            .toList()
            );
            orderEventProducer.publishOrderCreated(event);

            ordersCreatedCounter.increment();
            orderItemsCounter.increment(savedOrder.getItems().size());
            orderRevenueCounter.increment(totalAmount.doubleValue());

            log.info("Order created successfully: orderId={}, status={}", savedOrder.getId(), savedOrder.getStatus());
            return toResponse(savedOrder);
        });
    }

    @Cacheable(value = "order", key = "#orderId", sync = true)
    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByUser(String userEmail, Pageable pageable) {
        return orderRepository.findByUserEmailOrderByCreatedAtDesc(userEmail, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public void handleInventoryReserved(Long orderId, Long productId) {
        log.info("Marking inventory reserved: orderId={}, productId={}", orderId, productId);
        int updated = orderItemRepository.markInventoryReserved(orderId, productId);
        if (updated == 0) {
            log.warn("No order item found for orderId={}, productId={}", orderId, productId);
        }
    }

    @CacheEvict(value = "order", key = "#orderId")
    @Transactional
    public void handlePaymentCompleted(Long orderId) {
        log.info("Payment completed for orderId={}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        order.setStatus(OrderStatus.PAYMENT_COMPLETED);
        orderRepository.save(order);
        sagaCompletedCounter.increment();
    }

    @CacheEvict(value = "order", key = "#orderId")
    @Transactional
    public void handlePaymentFailed(Long orderId) {
        log.info("Payment failed for orderId={}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        order.setStatus(OrderStatus.PAYMENT_FAILED);
        orderRepository.save(order);
        sagaFailedCounter.increment();
    }

    @CacheEvict(value = "order", key = "#orderId")
    @Transactional
    public OrderResponse cancelOrder(Long orderId, String userEmail) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (!order.getUserEmail().equals(userEmail)) {
            throw new IllegalArgumentException("Order does not belong to this user");
        }

        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot cancel order in status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        ordersCancelledCounter.increment();
        log.info("Order cancelled: orderId={}", orderId);
        return toResponse(order);
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(i -> new OrderItemResponse(
                        i.getId(),
                        i.getProductId(),
                        i.getProductName(),
                        i.getQuantity(),
                        i.getUnitPrice(),
                        i.getSubtotal(),
                        i.isInventoryReserved()
                ))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getUserEmail(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getShippingAddress(),
                itemResponses,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
