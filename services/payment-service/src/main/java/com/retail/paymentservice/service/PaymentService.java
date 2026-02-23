package com.retail.paymentservice.service;

import com.retail.paymentservice.dto.PaymentResponse;
import com.retail.paymentservice.entity.Payment;
import com.retail.paymentservice.entity.PaymentStatus;
import com.retail.paymentservice.kafka.PaymentEventProducer;
import com.retail.paymentservice.kafka.event.OrderCreatedEvent;
import com.retail.paymentservice.kafka.event.PaymentCompletedEvent;
import com.retail.paymentservice.kafka.event.PaymentFailedEvent;
import com.retail.paymentservice.repository.PaymentRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final PaymentEventProducer paymentEventProducer;

    private final Counter paymentSuccessCounter;
    private final Counter paymentFailureCounter;
    private final Counter paymentIdempotentSkipCounter;
    private final Counter paymentRevenueCounter;
    private final Timer paymentProcessingTimer;

    public PaymentService(PaymentRepository paymentRepository,
                          PaymentGateway paymentGateway,
                          PaymentEventProducer paymentEventProducer,
                          MeterRegistry meterRegistry) {
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
        this.paymentEventProducer = paymentEventProducer;

        this.paymentSuccessCounter = Counter.builder("payments.success.total")
                .description("Total successful payments")
                .register(meterRegistry);
        this.paymentFailureCounter = Counter.builder("payments.failure.total")
                .description("Total failed payments")
                .register(meterRegistry);
        this.paymentIdempotentSkipCounter = Counter.builder("payments.idempotent.skip.total")
                .description("Duplicate payment requests skipped")
                .register(meterRegistry);
        this.paymentRevenueCounter = Counter.builder("payments.revenue.total")
                .description("Cumulative revenue from successful payments")
                .register(meterRegistry);
        this.paymentProcessingTimer = Timer.builder("payments.processing.duration")
                .description("Time to process a payment (gateway call + DB)")
                .register(meterRegistry);
    }

    @Transactional
    public void processPayment(OrderCreatedEvent event) {
        paymentProcessingTimer.record(() -> {
            log.info("Processing payment for orderId={}, amount={}", event.orderId(), event.totalAmount());

            if (paymentRepository.findByOrderId(event.orderId()).isPresent()) {
                log.warn("Payment already exists for orderId={}. Skipping (idempotent).", event.orderId());
                paymentIdempotentSkipCounter.increment();
                return;
            }

            Payment payment = Payment.builder()
                    .orderId(event.orderId())
                    .userEmail(event.userEmail())
                    .amount(event.totalAmount())
                    .status(PaymentStatus.PROCESSING)
                    .build();
            paymentRepository.save(payment);

            PaymentGateway.PaymentResult result = paymentGateway.charge(
                    event.orderId(), event.userEmail(), event.totalAmount());

            var itemDetails = event.items().stream()
                    .map(i -> new PaymentCompletedEvent.ItemDetail(i.productId(), i.quantity()))
                    .toList();

            if (result.success()) {
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setTransactionId(result.transactionId());
                payment.setCompletedAt(LocalDateTime.now());
                paymentRepository.save(payment);

                paymentEventProducer.publishPaymentCompleted(new PaymentCompletedEvent(
                        event.orderId(),
                        event.totalAmount(),
                        result.transactionId(),
                        itemDetails
                ));

                paymentSuccessCounter.increment();
                paymentRevenueCounter.increment(event.totalAmount().doubleValue());
                log.info("Payment completed: orderId={}, txnId={}", event.orderId(), result.transactionId());
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason(result.failureReason());
                paymentRepository.save(payment);

                var failedItems = event.items().stream()
                        .map(i -> new PaymentFailedEvent.ItemDetail(i.productId(), i.quantity()))
                        .toList();

                paymentEventProducer.publishPaymentFailed(new PaymentFailedEvent(
                        event.orderId(),
                        result.failureReason(),
                        failedItems
                ));

                paymentFailureCounter.increment();
                log.warn("Payment failed: orderId={}, reason={}", event.orderId(), result.failureReason());
            }
        });
    }

    @Cacheable(value = "payment", key = "#orderId", sync = true)
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElse(null);
        if (payment == null) {
            return null;
        }
        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentsByUser(String userEmail, Pageable pageable) {
        return paymentRepository.findByUserEmailOrderByCreatedAtDesc(userEmail, pageable)
                .map(this::toResponse);
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getUserEmail(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getTransactionId(),
                payment.getFailureReason(),
                payment.getCreatedAt(),
                payment.getCompletedAt()
        );
    }
}
