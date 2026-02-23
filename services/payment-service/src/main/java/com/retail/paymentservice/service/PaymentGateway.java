package com.retail.paymentservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Simulated payment gateway.
 *
 * In production, this would integrate with Stripe, PayPal, Adyen, etc.
 * For our local setup, it simulates:
 *   - 90% success rate (realistic for a payment gateway)
 *   - Random processing delay (50-200ms)
 *   - Generates a mock transaction ID on success
 *
 * To swap for a real gateway, implement a PaymentGateway interface
 * and inject the real implementation via a Spring profile.
 */
@Slf4j
@Component
public class PaymentGateway {

    private static final double SUCCESS_RATE = 0.90;

    public PaymentResult charge(Long orderId, String userEmail, BigDecimal amount) {
        log.info("Processing payment: orderId={}, email={}, amount={}", orderId, userEmail, amount);

        simulateLatency();

        boolean success = ThreadLocalRandom.current().nextDouble() < SUCCESS_RATE;

        if (success) {
            String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
            log.info("Payment succeeded: orderId={}, txnId={}", orderId, transactionId);
            return new PaymentResult(true, transactionId, null);
        } else {
            String reason = pickFailureReason();
            log.warn("Payment failed: orderId={}, reason={}", orderId, reason);
            return new PaymentResult(false, null, reason);
        }
    }

    private void simulateLatency() {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(50, 200));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String pickFailureReason() {
        String[] reasons = {
                "Insufficient funds",
                "Card declined",
                "Card expired",
                "Fraud detection triggered",
                "Gateway timeout"
        };
        return reasons[ThreadLocalRandom.current().nextInt(reasons.length)];
    }

    public record PaymentResult(boolean success, String transactionId, String failureReason) {}
}
