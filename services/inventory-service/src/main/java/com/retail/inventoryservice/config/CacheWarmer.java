package com.retail.inventoryservice.config;

import com.retail.inventoryservice.dto.ProductResponse;
import com.retail.inventoryservice.entity.Inventory;
import com.retail.inventoryservice.entity.Product;
import com.retail.inventoryservice.repository.InventoryRepository;
import com.retail.inventoryservice.repository.ProductRepository;
import com.retail.inventoryservice.service.ProductAccessTracker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cache.Cache;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Pre-warms the L2 (Dragonfly) product cache on startup.
 * Uses the sliding window access tracker to identify the top 100 hot products
 * from the last 6 hours. Falls back to a static top-100 query on first deploy
 * when no access data exists.
 */
@Slf4j
@Component
public class CacheWarmer implements ApplicationRunner {

    private final ProductAccessTracker accessTracker;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final RedisCacheManager redisCacheManager;

    public CacheWarmer(ProductAccessTracker accessTracker,
                       ProductRepository productRepository,
                       InventoryRepository inventoryRepository,
                       RedisCacheManager redisCacheManager) {
        this.accessTracker = accessTracker;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.redisCacheManager = redisCacheManager;
    }

    @Override
    public void run(ApplicationArguments args) {
        String source = "sliding window";
        List<Long> hotProducts = accessTracker.getHotProducts(100, 6);

        if (hotProducts.isEmpty()) {
            source = "static fallback";
            log.info("No access data found, falling back to static pre-warm");
            hotProducts = productRepository.findTop100ByActiveTrueOrderByIdAsc()
                    .stream().map(Product::getId).toList();
        }

        Cache productCache = redisCacheManager.getCache("product");
        if (productCache == null) {
            log.warn("Product cache not found in Redis cache manager, skipping pre-warm");
            return;
        }

        int count = 0;
        for (Long productId : hotProducts) {
            try {
                Product product = productRepository.findById(productId).orElse(null);
                if (product == null || !product.isActive()) continue;
                int stock = inventoryRepository.findByProductId(productId)
                        .map(Inventory::getAvailableStock)
                        .orElse(0);
                productCache.put(productId, new ProductResponse(
                        product.getId(),
                        product.getName(),
                        product.getDescription(),
                        product.getPrice(),
                        product.getCategory(),
                        product.getImageUrl(),
                        stock));
                count++;
            } catch (Exception e) {
                log.warn("Failed to pre-warm product {}: {}", productId, e.getMessage());
            }
        }
        log.info("Cache pre-warmed: {} products in L2/Dragonfly (source: {})", count, source);
    }
}
