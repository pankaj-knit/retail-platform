package com.retail.orderservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.grpc.client.ImportGrpcClients;

@SpringBootApplication
@EnableCaching
@ImportGrpcClients(basePackages = "com.retail.inventoryservice.proto")
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
