package com.retail.orderservice.kafka;

import com.retail.orderservice.kafka.event.OrderCreatedEvent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class OrderEventProducer {

    private static final String TOPIC_ORDER_CREATED = "order-created";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @CircuitBreaker(name = "kafkaProducer", fallbackMethod = "publishFallback")
    public void publishOrderCreated(OrderCreatedEvent event) {
        String key = event.orderId().toString();

        log.info("Publishing {}: orderId={}, totalAmount={}, items={}",
                TOPIC_ORDER_CREATED, event.orderId(), event.totalAmount(), event.items().size());

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(TOPIC_ORDER_CREATED, key, event);

        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                log.error("CRITICAL: Failed to publish {}: orderId={}. " +
                          "Order is saved in DB but event was NOT delivered.",
                        TOPIC_ORDER_CREATED, event.orderId(), throwable);
            } else {
                log.debug("Published {}: orderId={}, partition={}, offset={}",
                        TOPIC_ORDER_CREATED, event.orderId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }

    @SuppressWarnings("unused")
    private void publishFallback(OrderCreatedEvent event, Throwable t) {
        log.error("Circuit breaker OPEN for Kafka producer. Event not sent: " +
                  "orderId={}, totalAmount={}",
                event.orderId(), event.totalAmount(), t);
    }
}
