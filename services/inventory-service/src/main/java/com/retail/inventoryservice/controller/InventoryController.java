package com.retail.inventoryservice.controller;

import com.retail.inventoryservice.dto.ProductResponse;
import com.retail.inventoryservice.dto.ProductResponseV2;
import com.retail.inventoryservice.dto.StockCheckResponse;
import com.retail.inventoryservice.service.InventoryService;
import com.retail.inventoryservice.service.ProductAccessTracker;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class InventoryController {

    private final InventoryService inventoryService;
    private final ProductAccessTracker accessTracker;

    public InventoryController(InventoryService inventoryService,
                               ProductAccessTracker accessTracker) {
        this.inventoryService = inventoryService;
        this.accessTracker = accessTracker;
    }

    @GetMapping(path = "/products", version = "1")
    public ResponseEntity<Page<ProductResponse>> getProducts(
            @RequestParam(required = false) String category,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ProductResponse> products = (category != null)
                ? inventoryService.getProductsByCategory(category, pageable)
                : inventoryService.getAllProducts(pageable);
        return ResponseEntity.ok(products);
    }

    @GetMapping(path = "/products", version = "2")
    public ResponseEntity<Page<ProductResponseV2>> getProductsV2(
            @RequestParam(required = false) String category,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ProductResponseV2> products = (category != null)
                ? inventoryService.getProductsByCategoryV2(category, pageable)
                : inventoryService.getAllProductsV2(pageable);
        return ResponseEntity.ok(products);
    }

    @GetMapping(path = "/products/{id}", version = "1")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
        accessTracker.recordAccess(id);
        return ResponseEntity.ok(inventoryService.getProduct(id));
    }

    @GetMapping(path = "/inventory/{productId}", version = "1")
    public ResponseEntity<StockCheckResponse> checkStock(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.checkStock(productId));
    }

    @PostMapping(path = "/inventory/reserve", version = "1")
    public ResponseEntity<Boolean> reserveStock(
            @RequestParam @NotNull Long orderId,
            @RequestParam @NotNull Long productId,
            @RequestParam @Min(1) int quantity) {
        boolean reserved = inventoryService.reserveStock(orderId, productId, quantity);
        if (reserved) {
            return ResponseEntity.ok(true);
        }
        return ResponseEntity.badRequest().body(false);
    }
}
