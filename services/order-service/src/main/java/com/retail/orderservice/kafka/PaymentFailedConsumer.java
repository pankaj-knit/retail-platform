package com.retail.orderservice.kafka;

import com.retail.orderservice.kafka.event.PaymentFailedEvent;
import com.retail.orderservice.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentFailedConsumer {

    private final OrderService orderService;
    private final FailedEventPersister failedEventPersister;

    public PaymentFailedConsumer(OrderService orderService,
                                 FailedEventPersister failedEventPersister) {
        this.orderService = orderService;
        this.failedEventPersister = failedEventPersister;
    }

    @RetryableTopic(
            attempts = "3",
            backOff = @BackOff(delay = 1000, multiplier = 2.0),
            autoCreateTopics = "false"
    )
    @KafkaListener(topics = "payment-failed", groupId = "order-service")
    public void handle(PaymentFailedEvent event) {
        log.info("Received payment-failed: orderId={}, reason={}", event.orderId(), event.reason());
        orderService.handlePaymentFailed(event.orderId());
    }

    @DltHandler
    public void handleDlt(PaymentFailedEvent event,
                          @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("DLT: payment-failed event exhausted retries. orderId={}", event.orderId());
        failedEventPersister.persist(topic, event.orderId().toString(), event);
    }
}
