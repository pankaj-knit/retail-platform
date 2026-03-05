package com.retail.inventoryservice.config;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;

import java.util.Collection;
import java.util.Set;

/**
 * CacheManager that returns a TieredCache (L1 + L2) for configured cache names.
 * Used for "product" cache so gets check Caffeine then Dragonfly then DB.
 */
public class TieredCacheManager implements CacheManager {

    private final CaffeineCacheManager l1;
    private final RedisCacheManager l2;
    private final io.micrometer.core.instrument.MeterRegistry meterRegistry;
    private final Set<String> tieredCacheNames;

    public TieredCacheManager(CaffeineCacheManager l1,
                              RedisCacheManager l2,
                              io.micrometer.core.instrument.MeterRegistry meterRegistry) {
        this(l1, l2, meterRegistry, Set.of("product"));
    }

    public TieredCacheManager(CaffeineCacheManager l1,
                              RedisCacheManager l2,
                              io.micrometer.core.instrument.MeterRegistry meterRegistry,
                              Set<String> tieredCacheNames) {
        this.l1 = l1;
        this.l2 = l2;
        this.meterRegistry = meterRegistry;
        this.tieredCacheNames = tieredCacheNames;
    }

    @Override
    public Cache getCache(String name) {
        if (!tieredCacheNames.contains(name)) {
            return null;
        }
        Cache c1 = l1.getCache(name);
        Cache c2 = l2.getCache(name);
        if (c1 == null || c2 == null) {
            return c1 != null ? c1 : c2;
        }
        return new TieredCache(c1, c2, meterRegistry);
    }

    @Override
    public Collection<String> getCacheNames() {
        return tieredCacheNames;
    }
}
