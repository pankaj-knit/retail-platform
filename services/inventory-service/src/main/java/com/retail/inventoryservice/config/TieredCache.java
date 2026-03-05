package com.retail.inventoryservice.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.cache.Cache;

import java.util.concurrent.Callable;

/**
 * Two-tier cache: L1 (local, e.g. Caffeine) then L2 (shared, e.g. Redis/Dragonfly).
 * On get: L1 → L2 (promote to L1 on L2 hit) → null (DB load).
 * On put/evict: both tiers.
 * Emits metrics for cache_gets_total with result=l1_hit|l2_hit|miss for observability.
 */
public class TieredCache implements Cache {

    private final Cache l1;
    private final Cache l2;
    private final Counter l1HitCounter;
    private final Counter l2HitCounter;
    private final Counter missCounter;

    public TieredCache(Cache l1, Cache l2, MeterRegistry registry) {
        this.l1 = l1;
        this.l2 = l2;
        String cacheName = l1.getName() != null ? l1.getName() : "unknown";
        if (registry != null) {
            this.l1HitCounter = Counter.builder("cache.gets")
                    .tag("cache", cacheName)
                    .tag("result", "l1_hit")
                    .register(registry);
            this.l2HitCounter = Counter.builder("cache.gets")
                    .tag("cache", cacheName)
                    .tag("result", "l2_hit")
                    .register(registry);
            this.missCounter = Counter.builder("cache.gets")
                    .tag("cache", cacheName)
                    .tag("result", "miss")
                    .register(registry);
        } else {
            this.l1HitCounter = null;
            this.l2HitCounter = null;
            this.missCounter = null;
        }
    }

    @Override
    public String getName() {
        return l1.getName();
    }

    @Override
    public Object getNativeCache() {
        return l1.getNativeCache();
    }

    @Override
    public ValueWrapper get(Object key) {
        ValueWrapper w = l1.get(key);
        if (w != null) {
            if (l1HitCounter != null) l1HitCounter.increment();
            return w;
        }
        w = l2.get(key);
        if (w != null) {
            Object value = w.get();
            if (value != null) {
                l1.put(key, value);
            }
            if (l2HitCounter != null) l2HitCounter.increment();
            return w;
        }
        if (missCounter != null) missCounter.increment();
        return null;
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        ValueWrapper w = get(key);
        if (w == null) return null;
        Object value = w.get();
        if (value == null) return null;
        return type.cast(value);
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        ValueWrapper w = get(key);
        if (w != null) {
            @SuppressWarnings("unchecked")
            T value = (T) w.get();
            if (value != null) return value;
        }
        try {
            T value = valueLoader.call();
            put(key, value);
            return value;
        } catch (Exception e) {
            throw new ValueRetrievalException(key, valueLoader, e);
        }
    }

    @Override
    public void put(Object key, Object value) {
        l1.put(key, value);
        l2.put(key, value);
    }

    @Override
    public void evict(Object key) {
        l1.evict(key);
        l2.evict(key);
    }

    @Override
    public void clear() {
        l1.clear();
        l2.clear();
    }
}
