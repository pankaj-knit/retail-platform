package com.retail.inventoryservice.kafka;

import com.retail.inventoryservice.kafka.event.InventoryReservedEvent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class InventoryEventProducer {

    private static final String TOPIC_INVENTORY_RESERVED = "inventory-reserved";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public InventoryEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @CircuitBreaker(name = "kafkaProducer", fallbackMethod = "publishFallback")
    public void publishInventoryReserved(InventoryReservedEvent event) {
        String key = event.productId().toString();

        log.info("Publishing {}: orderId={}, productId={}, qty={}",
                TOPIC_INVENTORY_RESERVED, event.orderId(), event.productId(), event.quantity());

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(TOPIC_INVENTORY_RESERVED, key, event);

        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                log.error("CRITICAL: Failed to publish {}: orderId={}, productId={}, qty={}. " +
                          "Stock is reserved in DB but event was NOT delivered.",
                        TOPIC_INVENTORY_RESERVED, event.orderId(), event.productId(),
                        event.quantity(), throwable);
            } else {
                log.debug("Published {}: orderId={}, partition={}, offset={}",
                        TOPIC_INVENTORY_RESERVED, event.orderId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }

    @SuppressWarnings("unused")
    private void publishFallback(InventoryReservedEvent event, Throwable t) {
        log.error("Circuit breaker OPEN for Kafka producer. Event not sent: " +
                  "orderId={}, productId={}, qty={}. Cause: {}",
                event.orderId(), event.productId(), event.quantity(), t.getMessage());
    }
}
