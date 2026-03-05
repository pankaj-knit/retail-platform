package com.retail.inventoryservice.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Configuration
public class CacheConfig {

    private static final long BASE_TTL_NANOS = Duration.ofSeconds(5).toNanos();
    private static final long JITTER_NANOS = Duration.ofMillis(2000).toNanos();

    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration redisConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .computePrefixWith(name -> "inventory-service:" + name + "::")
                .serializeValuesWith(
                        SerializationPair.fromSerializer(GenericJacksonJsonRedisSerializer.builder().build()));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(redisConfig)
                .build();
    }

    @Bean
    public CaffeineCacheManager caffeineCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("product");
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfter(new Expiry<Object, Object>() {
                    @Override
                    public long expireAfterCreate(Object key, Object value, long currentTime) {
                        return BASE_TTL_NANOS + ThreadLocalRandom.current().nextLong(0, JITTER_NANOS);
                    }

                    @Override
                    public long expireAfterUpdate(Object key, Object value, long currentTime, long currentDuration) {
                        return currentDuration;
                    }

                    @Override
                    public long expireAfterRead(Object key, Object value, long currentTime, long currentDuration) {
                        return currentDuration;
                    }
                })
                .recordStats());
        return manager;
    }

    @Bean
    @Primary
    public CacheManager cacheManager(CaffeineCacheManager caffeineCacheManager,
                                    RedisCacheManager redisCacheManager,
                                    MeterRegistry meterRegistry) {
        return new TieredCacheManager(caffeineCacheManager, redisCacheManager, meterRegistry);
    }
}
