package com.innowise.paymentservice.client;

import com.innowise.paymentservice.dto.OrderInfoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "order-service", url = "${order-service.url}")
public interface OrderClient {
    @GetMapping("/api/orders/id/{id}")
    OrderInfoResponse getByOrderId(@PathVariable Long id);
}
