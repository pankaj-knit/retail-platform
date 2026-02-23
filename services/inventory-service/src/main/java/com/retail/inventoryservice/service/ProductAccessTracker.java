package com.retail.inventoryservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Tracks product access frequency using hourly-bucketed sorted sets in Dragonfly.
 * Each hour gets its own sorted set key (e.g. product:access:hour:2026022114).
 * Keys auto-expire after 25 hours for zero-maintenance cleanup.
 *
 * Used by CacheWarmer to identify hot products for L2 cache pre-warming.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductAccessTracker {

    private static final String BUCKET_PREFIX = "product:access:hour:";
    private static final DateTimeFormatter BUCKET_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHH");
    private static final Duration BUCKET_TTL = Duration.ofHours(25);

    private final StringRedisTemplate redisTemplate;

    public void recordAccess(Long productId) {
        try {
            String bucket = BUCKET_PREFIX + LocalDateTime.now().format(BUCKET_FORMAT);
            redisTemplate.opsForZSet().incrementScore(bucket, productId.toString(), 1);
            redisTemplate.expire(bucket, BUCKET_TTL);
        } catch (Exception e) {
            log.debug("Failed to record product access for {}: {}", productId, e.getMessage());
        }
    }

    public List<Long> getHotProducts(int topN, int lookbackHours) {
        Map<String, Double> scores = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < lookbackHours; i++) {
            String bucket = BUCKET_PREFIX + now.minusHours(i).format(BUCKET_FORMAT);
            Set<ZSetOperations.TypedTuple<String>> entries =
                    redisTemplate.opsForZSet().rangeWithScores(bucket, 0, -1);
            if (entries != null) {
                for (ZSetOperations.TypedTuple<String> entry : entries) {
                    if (entry.getValue() != null && entry.getScore() != null) {
                        scores.merge(entry.getValue(), entry.getScore(), Double::sum);
                    }
                }
            }
        }

        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topN)
                .map(e -> Long.parseLong(e.getKey()))
                .toList();
    }
}
