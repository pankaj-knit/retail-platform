package com.retail.inventoryservice.repository;

import com.retail.inventoryservice.entity.FailedEvent;
import com.retail.inventoryservice.entity.FailedEventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FailedEventRepository extends JpaRepository<FailedEvent, Long> {

    Page<FailedEvent> findByStatusOrderByCreatedAtDesc(FailedEventStatus status, Pageable pageable);

    Page<FailedEvent> findByStatusInOrderByCreatedAtDesc(List<FailedEventStatus> statuses, Pageable pageable);

    long countByStatus(FailedEventStatus status);
}
