package com.retail.paymentservice.controller;

import com.retail.paymentservice.dto.PaymentResponse;
import com.retail.paymentservice.service.PaymentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping(path = "/order/{orderId}", version = "1")
    public ResponseEntity<PaymentResponse> getPaymentByOrder(@PathVariable Long orderId) {
        PaymentResponse payment = paymentService.getPaymentByOrderId(orderId);
        if (payment == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(payment);
    }

    @GetMapping(version = "1")
    public ResponseEntity<Page<PaymentResponse>> getMyPayments(
            Authentication authentication,
            Pageable pageable) {
        String userEmail = authentication.getName();
        return ResponseEntity.ok(paymentService.getPaymentsByUser(userEmail, pageable));
    }
}
