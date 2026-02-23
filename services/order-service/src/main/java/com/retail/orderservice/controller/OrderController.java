package com.retail.orderservice.controller;

import com.retail.orderservice.dto.CreateOrderRequest;
import com.retail.orderservice.dto.OrderResponse;
import com.retail.orderservice.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping(version = "1")
    public ResponseEntity<OrderResponse> createOrder(
            Authentication authentication,
            @Valid @RequestBody CreateOrderRequest request) {
        String userEmail = authentication.getName();
        OrderResponse response = orderService.createOrder(userEmail, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping(path = "/{orderId}", version = "1")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrder(orderId));
    }

    @GetMapping(version = "1")
    public ResponseEntity<Page<OrderResponse>> getMyOrders(
            Authentication authentication,
            Pageable pageable) {
        String userEmail = authentication.getName();
        return ResponseEntity.ok(orderService.getOrdersByUser(userEmail, pageable));
    }

    @PostMapping(path = "/{orderId}/cancel", version = "1")
    public ResponseEntity<OrderResponse> cancelOrder(
            Authentication authentication,
            @PathVariable Long orderId) {
        String userEmail = authentication.getName();
        return ResponseEntity.ok(orderService.cancelOrder(orderId, userEmail));
    }
}
