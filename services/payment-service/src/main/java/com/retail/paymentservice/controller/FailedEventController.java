package com.retail.paymentservice.controller;

import com.retail.paymentservice.entity.FailedEvent;
import com.retail.paymentservice.entity.FailedEventStatus;
import com.retail.paymentservice.service.FailedEventService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/failed-events")
public class FailedEventController {

    private final FailedEventService failedEventService;

    public FailedEventController(FailedEventService failedEventService) {
        this.failedEventService = failedEventService;
    }

    @GetMapping(version = "1")
    public ResponseEntity<Page<FailedEvent>> getUnresolvedEvents(Pageable pageable) {
        return ResponseEntity.ok(failedEventService.getAllUnresolved(pageable));
    }

    @GetMapping(path = "/status/{status}", version = "1")
    public ResponseEntity<Page<FailedEvent>> getByStatus(
            @PathVariable FailedEventStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(failedEventService.getFailedEvents(status, pageable));
    }

    @GetMapping(path = "/count", version = "1")
    public ResponseEntity<Map<String, Long>> getFailedCount() {
        return ResponseEntity.ok(Map.of("failedCount", failedEventService.getFailedCount()));
    }

    @PostMapping(path = "/{id}/retry", version = "1")
    public ResponseEntity<FailedEvent> retryEvent(@PathVariable Long id) {
        return ResponseEntity.ok(failedEventService.markRetrying(id));
    }

    @PostMapping(path = "/{id}/resolve", version = "1")
    public ResponseEntity<FailedEvent> resolveEvent(@PathVariable Long id) {
        return ResponseEntity.ok(failedEventService.markResolved(id));
    }

    @PostMapping(path = "/{id}/discard", version = "1")
    public ResponseEntity<FailedEvent> discardEvent(@PathVariable Long id) {
        return ResponseEntity.ok(failedEventService.discard(id));
    }
}
