package com.retail.orderservice.kafka;

import com.retail.orderservice.entity.FailedEvent;
import com.retail.orderservice.entity.FailedEventStatus;
import com.retail.orderservice.repository.FailedEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
public class FailedEventPersister {

    private final FailedEventRepository failedEventRepository;
    private final ObjectMapper objectMapper;
    private final Counter dltEventsCounter;

    public FailedEventPersister(FailedEventRepository failedEventRepository,
                                ObjectMapper objectMapper,
                                MeterRegistry meterRegistry) {
        this.failedEventRepository = failedEventRepository;
        this.objectMapper = objectMapper;
        this.dltEventsCounter = Counter.builder("kafka.dlt.events.total")
                .description("Events sent to dead-letter topic")
                .register(meterRegistry);
    }

    public void persist(String topic, String key, Object event) {
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
