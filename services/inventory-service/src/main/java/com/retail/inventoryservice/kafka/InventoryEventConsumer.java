package com.retail.inventoryservice.kafka;

import com.retail.inventoryservice.kafka.event.PaymentCompletedEvent;
import com.retail.inventoryservice.kafka.event.PaymentFailedEvent;
import com.retail.inventoryservice.service.FailedEventService;
import com.retail.inventoryservice.service.InventoryService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for inventory-related events.
 *
 * Error handling flow:
 *   1. Message arrives on main topic (e.g., "payment-completed")
 *   2. If processing fails, @RetryableTopic retries 3 times with exponential backoff
 *      using separate retry topics (payment-completed-retry-0, -retry-1, -retry-2)
 *   3. If all retries fail, message goes to Dead Letter Topic (payment-completed-dlt)
 *   4. DLT handler persists the event to the failed_events database table
 *   5. Ops can review and manually retry via REST API
 */
@Slf4j
@Component
public class InventoryEventConsumer {

    private final InventoryService inventoryService;
    private final FailedEventService failedEventService;
    private final Counter dltEventsCounter;

    public InventoryEventConsumer(InventoryService inventoryService,
                                  FailedEventService failedEventService,
                                  MeterRegistry meterRegistry) {
        this.inventoryService = inventoryService;
        this.failedEventService = failedEventService;
        this.dltEventsCounter = Counter.builder("kafka.dlt.events.total")
                .description("Events sent to dead-letter topic")
                .register(meterRegistry);
    }

    @RetryableTopic(
            attempts = "4",
            backOff = @BackOff(delay = 1000, multiplier = 2.0, maxDelay = 10000),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltStrategy = DltStrategy.ALWAYS_RETRY_ON_ERROR,
            autoCreateTopics = "false"
    )
    @KafkaListener(topics = "payment-completed", groupId = "inventory-service")
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("Processing payment-completed: orderId={}, items={}",
                event.orderId(), event.items().size());

        try {
            for (PaymentCompletedEvent.ItemDetail item : event.items()) {
                inventoryService.confirmDeduction(item.productId(), item.quantity());
                log.debug("Confirmed deduction: orderId={}, productId={}, qty={}",
                        event.orderId(), item.productId(), item.quantity());
            }
            log.info("Successfully processed payment-completed: orderId={}", event.orderId());
        } catch (Exception e) {
            log.error("Failed to process payment-completed: orderId={}", event.orderId(), e);
            throw e;
        }
    }

    @RetryableTopic(
            attempts = "4",
            backOff = @BackOff(delay = 1000, multiplier = 2.0, maxDelay = 10000),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltStrategy = DltStrategy.ALWAYS_RETRY_ON_ERROR,
            autoCreateTopics = "false"
    )
    @KafkaListener(topics = "payment-failed", groupId = "inventory-service")
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.info("Processing payment-failed: orderId={}, items={}",
                event.orderId(), event.items().size());

        try {
            for (PaymentFailedEvent.ItemDetail item : event.items()) {
                inventoryService.releaseReservation(item.productId(), item.quantity());
                log.debug("Released reservation: orderId={}, productId={}, qty={}",
                        event.orderId(), item.productId(), item.quantity());
            }
            log.info("Successfully processed payment-failed: orderId={}", event.orderId());
        } catch (Exception e) {
            log.error("Failed to process payment-failed: orderId={}", event.orderId(), e);
            throw e;
        }
    }

    /**
     * DLT handler: persists failed payment-completed events to the database.
     * After all Kafka-level retries are exhausted, the event lands here.
     * We save it to the failed_events table so ops can query, review, and retry.
     */
    @KafkaListener(topics = "payment-completed-dlt", groupId = "inventory-service")
    public void handlePaymentCompletedDlt(PaymentCompletedEvent event) {
        log.error("DLT: payment-completed exhausted all retries. " +
                  "Persisting to DB: orderId={}, items={}",
                event.orderId(), event.items().size());
        dltEventsCounter.increment();

        failedEventService.persistFailedEvent(
                "payment-completed",
                event.orderId().toString(),
                event,
                new RuntimeException("Exhausted all Kafka retries for payment-completed")
        );
    }

    @KafkaListener(topics = "payment-failed-dlt", groupId = "inventory-service")
    public void handlePaymentFailedDlt(PaymentFailedEvent event) {
        log.error("DLT: payment-failed exhausted all retries. " +
                  "Persisting to DB: orderId={}, items={}. " +
                  "Stock may remain reserved until manually resolved.",
                event.orderId(), event.items().size());
        dltEventsCounter.increment();

        failedEventService.persistFailedEvent(
                "payment-failed",
                event.orderId().toString(),
                event,
                new RuntimeException("Exhausted all Kafka retries for payment-failed")
        );
    }
}
