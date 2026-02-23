package com.retail.paymentservice.service;

import com.retail.paymentservice.entity.FailedEvent;
import com.retail.paymentservice.entity.FailedEventStatus;
import com.retail.paymentservice.repository.FailedEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class FailedEventService {

    private final FailedEventRepository failedEventRepository;

    public FailedEventService(FailedEventRepository failedEventRepository) {
        this.failedEventRepository = failedEventRepository;
    }

    @Transactional(readOnly = true)
    public Page<FailedEvent> getFailedEvents(FailedEventStatus status, Pageable pageable) {
        return failedEventRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
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

    @Transactional
    public FailedEvent markRetrying(Long id) {
        FailedEvent event = failedEventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Failed event not found: " + id));
        event.setStatus(FailedEventStatus.RETRYING);
        event.setRetryCount(event.getRetryCount() + 1);
        return failedEventRepository.save(event);
    }

    @Transactional
    public FailedEvent markResolved(Long id) {
        FailedEvent event = failedEventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Failed event not found: " + id));
        event.setStatus(FailedEventStatus.RESOLVED);
        event.setResolvedAt(LocalDateTime.now());
        return failedEventRepository.save(event);
    }

    @Transactional
    public FailedEvent discard(Long id) {
        FailedEvent event = failedEventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Failed event not found: " + id));
        event.setStatus(FailedEventStatus.DISCARDED);
        event.setResolvedAt(LocalDateTime.now());
        return failedEventRepository.save(event);
    }
}
