package com.retail.paymentservice.kafka;

import com.retail.paymentservice.kafka.event.PaymentCompletedEvent;
import com.retail.paymentservice.kafka.event.PaymentFailedEvent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class PaymentEventProducer {

    private static final String TOPIC_PAYMENT_COMPLETED = "payment-completed";
    private static final String TOPIC_PAYMENT_FAILED = "payment-failed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @CircuitBreaker(name = "kafkaProducer", fallbackMethod = "publishCompletedFallback")
    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        String key = event.orderId().toString();
        log.info("Publishing {}: orderId={}, txnId={}", TOPIC_PAYMENT_COMPLETED, event.orderId(), event.transactionId());

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(TOPIC_PAYMENT_COMPLETED, key, event);

        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                log.error("CRITICAL: Failed to publish {}: orderId={}. Payment recorded in DB but event NOT delivered.",
                        TOPIC_PAYMENT_COMPLETED, event.orderId(), throwable);
            } else {
                log.debug("Published {}: orderId={}, partition={}, offset={}",
                        TOPIC_PAYMENT_COMPLETED, event.orderId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }

    @CircuitBreaker(name = "kafkaProducer", fallbackMethod = "publishFailedFallback")
    public void publishPaymentFailed(PaymentFailedEvent event) {
        String key = event.orderId().toString();
        log.info("Publishing {}: orderId={}, items={}", TOPIC_PAYMENT_FAILED, event.orderId(), event.items().size());

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(TOPIC_PAYMENT_FAILED, key, event);

        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                log.error("CRITICAL: Failed to publish {}: orderId={}. " +
                          "Stock remains reserved until manual intervention.",
                        TOPIC_PAYMENT_FAILED, event.orderId(), throwable);
            } else {
                log.debug("Published {}: orderId={}, partition={}, offset={}",
                        TOPIC_PAYMENT_FAILED, event.orderId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }

    @SuppressWarnings("unused")
    private void publishCompletedFallback(PaymentCompletedEvent event, Throwable t) {
        log.error("Circuit breaker OPEN for Kafka producer. payment-completed not sent: orderId={}, cause: {}",
                event.orderId(), t.getMessage());
    }

    @SuppressWarnings("unused")
    private void publishFailedFallback(PaymentFailedEvent event, Throwable t) {
        log.error("Circuit breaker OPEN for Kafka producer. payment-failed not sent: orderId={}, cause: {}",
                event.orderId(), t.getMessage());
    }
}
