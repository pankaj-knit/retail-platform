package com.retail.paymentservice.kafka;

import com.retail.paymentservice.entity.FailedEvent;
import com.retail.paymentservice.entity.FailedEventStatus;
import com.retail.paymentservice.kafka.event.OrderCreatedEvent;
import com.retail.paymentservice.repository.FailedEventRepository;
import com.retail.paymentservice.service.PaymentService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
public class PaymentEventConsumer {

    private final PaymentService paymentService;
    private final FailedEventRepository failedEventRepository;
    private final ObjectMapper objectMapper;
    private final Counter dltEventsCounter;

    public PaymentEventConsumer(PaymentService paymentService,
                                FailedEventRepository failedEventRepository,
                                ObjectMapper objectMapper,
                                MeterRegistry meterRegistry) {
        this.paymentService = paymentService;
        this.failedEventRepository = failedEventRepository;
        this.objectMapper = objectMapper;
        this.dltEventsCounter = Counter.builder("kafka.dlt.events.total")
                .description("Events sent to dead-letter topic")
                .register(meterRegistry);
    }

    @RetryableTopic(
            attempts = "3",
            backOff = @BackOff(delay = 1000, multiplier = 2.0),
            autoCreateTopics = "false"
    )
    @KafkaListener(topics = "order-created", groupId = "payment-service")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received order-created: orderId={}, amount={}, items={}",
                event.orderId(), event.totalAmount(), event.items().size());
        paymentService.processPayment(event);
    }

    @DltHandler
    public void handleOrderCreatedDlt(OrderCreatedEvent event,
                                      @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("DLT: order-created event exhausted retries. orderId={}, amount={}",
                event.orderId(), event.totalAmount());
        persistFailedEvent(topic, event.orderId().toString(), event);
    }

    private void persistFailedEvent(String topic, String key, Object event) {
        dltEventsCounter.increment();
        try {
            String payload = objectMapper.writeValueAsString(event);
            failedEventRepository.save(FailedEvent.builder()
                    .topic(topic)
                    .eventKey(key)
                    .payload(payload)
                    .errorMessage("Exhausted all retry attempts")
                    .status(FailedEventStatus.FAILED)
                    .build());
            log.info("Persisted failed event to DB: topic={}, key={}", topic, key);
        } catch (Exception e) {
            log.error("CRITICAL: Failed to persist DLT event to DB. topic={}, key={}", topic, key, e);
        }
    }
}
