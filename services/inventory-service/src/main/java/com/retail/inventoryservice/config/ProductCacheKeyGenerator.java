package com.retail.inventoryservice.config;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Generates versioned cache keys for the product cache so schema/API changes
 * can invalidate all entries by bumping the version.
 * Format: product:v{version}:{productId}
 */
@Component("productCacheKeyGenerator")
public class ProductCacheKeyGenerator implements KeyGenerator {

    @Value("${cache.product.schema-version:1}")
    private int schemaVersion;

    @Override
    public Object generate(Object target, Method method, Object... params) {
        Long productId = resolveProductId(method.getName(), params);
        return productId != null ? keyForProduct(productId) : null;
    }

    /**
     * Builds the versioned cache key for a product. Used by CacheWarmer to pre-warm with the same key format.
     */
    public String keyForProduct(Long productId) {
        return "product:v" + schemaVersion + ":" + productId;
    }

    private static Long resolveProductId(String methodName, Object... params) {
        switch (methodName) {
            case "getProduct":
                return params.length > 0 && params[0] instanceof Long ? (Long) params[0] : null;
            case "reserveStock":
                return params.length > 1 && params[1] instanceof Long ? (Long) params[1] : null;
            case "confirmDeduction":
            case "releaseReservation":
                return params.length > 0 && params[0] instanceof Long ? (Long) params[0] : null;
            default:
                return params.length > 0 && params[0] instanceof Long ? (Long) params[0] : null;
        }
    }
}
