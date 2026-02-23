package com.retail.inventoryservice.service;

import com.retail.inventoryservice.entity.FailedEvent;
import com.retail.inventoryservice.entity.FailedEventStatus;
import com.retail.inventoryservice.kafka.event.PaymentCompletedEvent;
import com.retail.inventoryservice.kafka.event.PaymentFailedEvent;
import com.retail.inventoryservice.repository.FailedEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class FailedEventService {

    private final FailedEventRepository failedEventRepository;
    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;

    public FailedEventService(FailedEventRepository failedEventRepository,
                              InventoryService inventoryService,
                              ObjectMapper objectMapper) {
        this.failedEventRepository = failedEventRepository;
        this.inventoryService = inventoryService;
        this.objectMapper = objectMapper;
    }

    /**
     * Persist a failed event to the database for later review and retry.
     * Called by DLT handlers after all Kafka-level retries are exhausted.
     */
    @Transactional
    public FailedEvent persistFailedEvent(String topic, String eventKey,
                                          Object event, Throwable error) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            String errorMsg = error != null ? error.getMessage() : "Unknown error";

            FailedEvent failedEvent = FailedEvent.builder()
                    .topic(topic)
                    .eventKey(eventKey)
                    .payload(payload)
                    .errorMessage(errorMsg)
                    .build();

            FailedEvent saved = failedEventRepository.save(failedEvent);
            log.warn("Persisted failed event: id={}, topic={}, key={}",
                    saved.getId(), topic, eventKey);
            return saved;
        } catch (Exception e) {
            // Last resort: if we can't even save to DB, log everything
            log.error("CRITICAL: Failed to persist failed event to DB. " +
                      "topic={}, key={}, event={}, originalError={}",
                    topic, eventKey, event, error, e);
            return null;
        }
    }

    @Transactional(readOnly = true)
    public Page<FailedEvent> getFailedEvents(Pageable pageable) {
        return failedEventRepository.findByStatusOrderByCreatedAtDesc(FailedEventStatus.FAILED, pageable);
    }

    @Transactional(readOnly = true)
    public Page<FailedEvent> getAllUnresolved(Pageable pageable) {
        return failedEventRepository.findByStatusInOrderByCreatedAtDesc(
                List.of(FailedEventStatus.FAILED, FailedEventStatus.RETRYING), pageable);
    }

    @Transactional(readOnly = true)
    public long getFailedCount() {
        return failedEventRepository.countByStatus(FailedEventStatus.FAILED);
    }

    /**
     * Manually retry a failed event. Reads the payload from DB,
     * deserializes it, and re-processes through the business logic.
     */
    @Transactional
    public boolean retryEvent(Long failedEventId) {
        FailedEvent failedEvent = failedEventRepository.findById(failedEventId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Failed event not found: " + failedEventId));

        if (failedEvent.getRetryCount() >= failedEvent.getMaxRetries()) {
            log.warn("Max retries exceeded for failed event: id={}", failedEventId);
            return false;
        }

        failedEvent.setStatus(FailedEventStatus.RETRYING);
        failedEvent.setRetryCount(failedEvent.getRetryCount() + 1);
        failedEventRepository.save(failedEvent);

        try {
            processEvent(failedEvent);
            failedEvent.setStatus(FailedEventStatus.RESOLVED);
            failedEvent.setResolvedAt(LocalDateTime.now());
            failedEventRepository.save(failedEvent);
            log.info("Successfully retried failed event: id={}", failedEventId);
            return true;
        } catch (Exception e) {
            failedEvent.setStatus(FailedEventStatus.FAILED);
            failedEvent.setErrorMessage(e.getMessage());
            failedEventRepository.save(failedEvent);
            log.error("Retry failed for event: id={}", failedEventId, e);
            return false;
        }
    }

    @Transactional
    public void discardEvent(Long failedEventId) {
        FailedEvent failedEvent = failedEventRepository.findById(failedEventId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Failed event not found: " + failedEventId));
        failedEvent.setStatus(FailedEventStatus.DISCARDED);
        failedEvent.setResolvedAt(LocalDateTime.now());
        failedEventRepository.save(failedEvent);
        log.info("Discarded failed event: id={}", failedEventId);
    }

    private void processEvent(FailedEvent failedEvent) throws Exception {
        switch (failedEvent.getTopic()) {
            case "payment-completed", "payment-completed-dlt" -> {
                PaymentCompletedEvent event = objectMapper.readValue(
                        failedEvent.getPayload(), PaymentCompletedEvent.class);
                for (PaymentCompletedEvent.ItemDetail item : event.items()) {
                    inventoryService.confirmDeduction(item.productId(), item.quantity());
                }
            }
            case "payment-failed", "payment-failed-dlt" -> {
                PaymentFailedEvent event = objectMapper.readValue(
                        failedEvent.getPayload(), PaymentFailedEvent.class);
                for (PaymentFailedEvent.ItemDetail item : event.items()) {
                    inventoryService.releaseReservation(item.productId(), item.quantity());
                }
            }
            default -> throw new IllegalArgumentException(
                    "Unknown topic: " + failedEvent.getTopic());
        }
    }
}
