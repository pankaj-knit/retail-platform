package com.retail.inventoryservice.service;

import com.retail.inventoryservice.dto.ProductResponse;
import com.retail.inventoryservice.dto.ProductResponseV2;
import com.retail.inventoryservice.dto.StockCheckResponse;
import com.retail.inventoryservice.dto.StockStatus;
import com.retail.inventoryservice.entity.Inventory;
import com.retail.inventoryservice.entity.Product;
import com.retail.inventoryservice.kafka.InventoryEventProducer;
import com.retail.inventoryservice.kafka.event.InventoryReservedEvent;
import com.retail.inventoryservice.repository.InventoryRepository;
import com.retail.inventoryservice.repository.ProductRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class InventoryService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryEventProducer eventProducer;

    private final Counter stockReservedCounter;
    private final Counter stockReservationFailedCounter;
    private final Counter stockDeductionConfirmedCounter;
    private final Counter stockReleasedCounter;
    private final Counter stockUnitsReservedCounter;

    public InventoryService(ProductRepository productRepository,
                            InventoryRepository inventoryRepository,
                            InventoryEventProducer eventProducer,
                            MeterRegistry meterRegistry) {
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.eventProducer = eventProducer;

        this.stockReservedCounter = Counter.builder("inventory.reservations.success.total")
                .description("Successful stock reservations")
                .register(meterRegistry);
        this.stockReservationFailedCounter = Counter.builder("inventory.reservations.failed.total")
                .description("Failed stock reservations (insufficient stock)")
                .register(meterRegistry);
        this.stockDeductionConfirmedCounter = Counter.builder("inventory.deductions.confirmed.total")
                .description("Stock deductions confirmed after payment")
                .register(meterRegistry);
        this.stockReleasedCounter = Counter.builder("inventory.releases.total")
                .description("Stock released after payment failure")
                .register(meterRegistry);
        this.stockUnitsReservedCounter = Counter.builder("inventory.units.reserved.total")
                .description("Total units reserved across all reservations")
                .register(meterRegistry);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        Page<Product> products = productRepository.findByActiveTrue(pageable);
        return mapProductPage(products);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductsByCategory(String category, Pageable pageable) {
        Page<Product> products = productRepository.findByCategoryAndActiveTrue(category, pageable);
        return mapProductPage(products);
    }

    private Page<ProductResponse> mapProductPage(Page<Product> productPage) {
        List<Product> products = productPage.getContent();
        if (products.isEmpty()) {
            return productPage.map(p -> toProductResponse(p, 0));
        }
        List<Long> productIds = products.stream().map(Product::getId).toList();
        Map<Long, Inventory> inventoryMap = inventoryRepository.findByProductIdIn(productIds)
                .stream()
                .collect(Collectors.toMap(Inventory::getProductId, Function.identity()));

        return productPage.map(p -> {
            int available = inventoryMap.containsKey(p.getId())
                    ? inventoryMap.get(p.getId()).getAvailableStock()
                    : 0;
            return toProductResponse(p, available);
        });
    }

    @Cacheable(value = "product", key = "#productId", sync = true)
    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        int available = inventoryRepository.findByProductId(product.getId())
                .map(Inventory::getAvailableStock)
                .orElse(0);
        return toProductResponse(product, available);
    }

    @Transactional(readOnly = true)
    public StockCheckResponse checkStock(Long productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException("Inventory not found for product: " + productId));

        int available = inventory.getAvailableStock();
        return new StockCheckResponse(productId, available, available > 0);
    }

    /**
     * Reserve stock for an order. Called by Order Service via REST.
     *
     * Uses the atomic reserveStock query to prevent race conditions.
     * If successful, publishes an InventoryReserved event to Kafka.
     */
    @CacheEvict(value = "product", key = "#productId")
    @Transactional
    public boolean reserveStock(Long orderId, Long productId, int quantity) {
        int updated = inventoryRepository.reserveStock(productId, quantity);
        if (updated == 0) {
            log.warn("Failed to reserve stock: productId={}, qty={} (insufficient stock)", productId, quantity);
            stockReservationFailedCounter.increment();
            return false;
        }

        log.info("Stock reserved: productId={}, qty={}", productId, quantity);
        stockReservedCounter.increment();
        stockUnitsReservedCounter.increment(quantity);
        eventProducer.publishInventoryReserved(new InventoryReservedEvent(orderId, productId, quantity));
        return true;
    }

    /**
     * Confirm stock deduction after successful payment.
     * Called when "payment-completed" event is received from Kafka.
     */
    @CacheEvict(value = "product", key = "#productId")
    @Transactional
    public void confirmDeduction(Long productId, int quantity) {
        int updated = inventoryRepository.confirmStockDeduction(productId, quantity);
        if (updated == 0) {
            log.error("Failed to confirm stock deduction: productId={}, qty={}", productId, quantity);
        } else {
            stockDeductionConfirmedCounter.increment();
            log.info("Stock deduction confirmed: productId={}, qty={}", productId, quantity);
        }
    }

    /**
     * Release reserved stock after failed payment or order cancellation.
     * Called when "payment-failed" event is received from Kafka.
     */
    @CacheEvict(value = "product", key = "#productId")
    @Transactional
    public void releaseReservation(Long productId, int quantity) {
        int updated = inventoryRepository.releaseStock(productId, quantity);
        if (updated == 0) {
            log.error("Failed to release reservation: productId={}, qty={}", productId, quantity);
        } else {
            stockReleasedCounter.increment();
            log.info("Reservation released: productId={}, qty={}", productId, quantity);
        }
    }

    @Transactional(readOnly = true)
    public Page<ProductResponseV2> getAllProductsV2(Pageable pageable) {
        Page<Product> products = productRepository.findByActiveTrue(pageable);
        return mapProductPageV2(products);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponseV2> getProductsByCategoryV2(String category, Pageable pageable) {
        Page<Product> products = productRepository.findByCategoryAndActiveTrue(category, pageable);
        return mapProductPageV2(products);
    }

    private Page<ProductResponseV2> mapProductPageV2(Page<Product> productPage) {
        List<Product> products = productPage.getContent();
        if (products.isEmpty()) {
            return productPage.map(p -> toProductResponseV2(p, 0));
        }
        List<Long> productIds = products.stream().map(Product::getId).toList();
        Map<Long, Inventory> inventoryMap = inventoryRepository.findByProductIdIn(productIds)
                .stream()
                .collect(Collectors.toMap(Inventory::getProductId, Function.identity()));

        return productPage.map(p -> {
            int available = inventoryMap.containsKey(p.getId())
                    ? inventoryMap.get(p.getId()).getAvailableStock()
                    : 0;
            return toProductResponseV2(p, available);
        });
    }

    private ProductResponse toProductResponse(Product product, int availableStock) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getCategory(),
                product.getImageUrl(),
                availableStock
        );
    }

    private ProductResponseV2 toProductResponseV2(Product product, int availableStock) {
        StockStatus status;
        if (availableStock <= 0) {
            status = StockStatus.OUT_OF_STOCK;
        } else if (availableStock <= 10) {
            status = StockStatus.LOW_STOCK;
        } else {
            status = StockStatus.IN_STOCK;
        }

        return new ProductResponseV2(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getCategory(),
                product.getImageUrl(),
                availableStock,
                status
        );
    }
}
