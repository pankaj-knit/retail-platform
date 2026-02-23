package com.retail.orderservice.grpc;

import com.retail.inventoryservice.proto.CheckStockRequest;
import com.retail.inventoryservice.proto.CheckStockResponse;
import com.retail.inventoryservice.proto.InventoryGrpcGrpc;
import com.retail.inventoryservice.proto.ReserveStockRequest;
import com.retail.inventoryservice.proto.ReserveStockResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * gRPC client for internal calls to Inventory Service.
 * Spring gRPC auto-creates blocking stub beans via @ImportGrpcClients.
 * The stub is bound to the "inventory-service" channel in application.yaml.
 */
@Slf4j
@Service
public class InventoryGrpcClient {

    private final InventoryGrpcGrpc.InventoryGrpcBlockingStub inventoryStub;

    public InventoryGrpcClient(InventoryGrpcGrpc.InventoryGrpcBlockingStub inventoryStub) {
        this.inventoryStub = inventoryStub;
    }

    @CircuitBreaker(name = "inventoryGrpc", fallbackMethod = "checkStockFallback")
    public CheckStockResponse checkStock(Long productId) {
        log.debug("gRPC checkStock: productId={}", productId);
        return inventoryStub.checkStock(
                CheckStockRequest.newBuilder()
                        .setProductId(productId)
                        .build()
        );
    }

    @CircuitBreaker(name = "inventoryGrpc", fallbackMethod = "reserveStockFallback")
    public ReserveStockResponse reserveStock(Long orderId, Long productId, int quantity) {
        log.info("gRPC reserveStock: orderId={}, productId={}, qty={}", orderId, productId, quantity);
        return inventoryStub.reserveStock(
                ReserveStockRequest.newBuilder()
                        .setOrderId(orderId)
                        .setProductId(productId)
                        .setQuantity(quantity)
                        .build()
        );
    }

    @SuppressWarnings("unused")
    private CheckStockResponse checkStockFallback(Long productId, Throwable t) {
        log.error("Circuit breaker OPEN for inventory gRPC checkStock. productId={}, cause: {}",
                productId, t.getMessage());
        throw new RuntimeException("Inventory service unavailable. Please try again later.");
    }

    @SuppressWarnings("unused")
    private ReserveStockResponse reserveStockFallback(Long orderId, Long productId, int quantity, Throwable t) {
        log.error("Circuit breaker OPEN for inventory gRPC reserveStock. orderId={}, productId={}, cause: {}",
                orderId, productId, t.getMessage());
        throw new RuntimeException("Inventory service unavailable. Cannot reserve stock.");
    }
}
