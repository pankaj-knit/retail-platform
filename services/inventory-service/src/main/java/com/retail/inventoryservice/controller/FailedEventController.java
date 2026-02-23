package com.retail.inventoryservice.controller;

import com.retail.inventoryservice.entity.FailedEvent;
import com.retail.inventoryservice.service.FailedEventService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
    public ResponseEntity<Page<FailedEvent>> getUnresolvedEvents(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(failedEventService.getAllUnresolved(pageable));
    }

    @GetMapping(path = "/failed", version = "1")
    public ResponseEntity<Page<FailedEvent>> getFailedEvents(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(failedEventService.getFailedEvents(pageable));
    }

    @GetMapping(path = "/count", version = "1")
    public ResponseEntity<Map<String, Long>> getFailedCount() {
        return ResponseEntity.ok(Map.of("failedCount", failedEventService.getFailedCount()));
    }

    @PostMapping(path = "/{id}/retry", version = "1")
    public ResponseEntity<Map<String, Object>> retryEvent(@PathVariable Long id) {
        boolean success = failedEventService.retryEvent(id);
        return ResponseEntity.ok(Map.of(
                "id", id,
                "retrySuccess", success
        ));
    }

    @PostMapping(path = "/{id}/discard", version = "1")
    public ResponseEntity<Map<String, Object>> discardEvent(@PathVariable Long id) {
        failedEventService.discardEvent(id);
        return ResponseEntity.ok(Map.of(
                "id", id,
                "status", "DISCARDED"
        ));
    }
}
