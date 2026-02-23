package com.retail.orderservice.repository;

import com.retail.orderservice.entity.FailedEvent;
import com.retail.orderservice.entity.FailedEventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FailedEventRepository extends JpaRepository<FailedEvent, Long> {

    Page<FailedEvent> findByStatusOrderByCreatedAtDesc(FailedEventStatus status, Pageable pageable);

    Page<FailedEvent> findByStatusInOrderByCreatedAtDesc(List<FailedEventStatus> statuses, Pageable pageable);

    long countByStatus(FailedEventStatus status);
}
