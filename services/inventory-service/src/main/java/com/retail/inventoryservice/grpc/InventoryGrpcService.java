package com.retail.inventoryservice.grpc;

import com.retail.inventoryservice.dto.StockCheckResponse;
import com.retail.inventoryservice.proto.*;
import com.retail.inventoryservice.service.InventoryService;
import io.grpc.Context;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * gRPC server implementation for internal service-to-service calls.
 *
 * This is a thin adapter layer: it converts protobuf request objects into
 * calls to the shared InventoryService business logic, then converts the
 * result back to protobuf response objects.
 *
 * Spring gRPC auto-detects this @Service (it implements BindableService
 * via InventoryGrpcImplBase) and registers it on the gRPC server (port 9090).
 */
@Slf4j
@Service
public class InventoryGrpcService extends InventoryGrpcGrpc.InventoryGrpcImplBase {

    private final InventoryService inventoryService;

    public InventoryGrpcService(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Override
    public void checkStock(CheckStockRequest request,
                           StreamObserver<CheckStockResponse> responseObserver) {
        if (Context.current().isCancelled()) {
            responseObserver.onError(Status.CANCELLED.withDescription("Client cancelled").asRuntimeException());
            return;
        }
        try {
            long productId = request.getProductId();
            log.debug("gRPC CheckStock: productId={}", productId);

            StockCheckResponse stock = inventoryService.checkStock(productId);

            CheckStockResponse response = CheckStockResponse.newBuilder()
                    .setProductId(stock.productId())
                    .setAvailableStock(stock.availableStock())
                    .setInStock(stock.inStock())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription(e.getMessage())
                            .asRuntimeException());
        } catch (Exception e) {
            log.error("gRPC CheckStock failed", e);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Internal error")
                            .asRuntimeException());
        }
    }

    @Override
    public void reserveStock(ReserveStockRequest request,
                             StreamObserver<ReserveStockResponse> responseObserver) {
        if (Context.current().isCancelled()) {
            responseObserver.onError(Status.CANCELLED.withDescription("Client cancelled").asRuntimeException());
            return;
        }
        try {
            long orderId = request.getOrderId();
            long productId = request.getProductId();
            int quantity = request.getQuantity();
            log.debug("gRPC ReserveStock: orderId={}, productId={}, qty={}", orderId, productId, quantity);

            boolean reserved = inventoryService.reserveStock(orderId, productId, quantity);

            ReserveStockResponse response = ReserveStockResponse.newBuilder()
                    .setSuccess(reserved)
                    .setMessage(reserved ? "Stock reserved" : "Insufficient stock")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC ReserveStock failed", e);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Internal error")
                            .asRuntimeException());
        }
    }
}
