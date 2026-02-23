package com.retail.orderservice.kafka;

import com.retail.orderservice.kafka.event.InventoryReservedEvent;
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
public class InventoryReservedConsumer {

    private final OrderService orderService;
    private final FailedEventPersister failedEventPersister;

    public InventoryReservedConsumer(OrderService orderService,
                                     FailedEventPersister failedEventPersister) {
        this.orderService = orderService;
        this.failedEventPersister = failedEventPersister;
    }

    @RetryableTopic(
            attempts = "3",
            backOff = @BackOff(delay = 1000, multiplier = 2.0),
            autoCreateTopics = "false"
    )
    @KafkaListener(topics = "inventory-reserved", groupId = "order-service")
    public void handle(InventoryReservedEvent event) {
        log.info("Received inventory-reserved: orderId={}, productId={}, qty={}",
                event.orderId(), event.productId(), event.quantity());
        orderService.handleInventoryReserved(event.orderId(), event.productId());
    }

    @DltHandler
    public void handleDlt(InventoryReservedEvent event,
                          @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("DLT: inventory-reserved event exhausted retries. orderId={}, productId={}",
                event.orderId(), event.productId());
        failedEventPersister.persist(topic, event.orderId().toString(), event);
    }
}
