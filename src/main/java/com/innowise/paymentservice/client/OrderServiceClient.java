package com.innowise.paymentservice.client;

import com.innowise.paymentservice.dto.OrderInfoResponse;
import com.innowise.paymentservice.exception.OrderServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderServiceClient {
    private static final Logger log = LoggerFactory.getLogger(OrderServiceClient.class);
    private final OrderClient orderClient;

    @CircuitBreaker(name = "orderService", fallbackMethod = "fallbackGetByOrderId")
    public OrderInfoResponse getByOrderId(Long orderId) { return orderClient.getByOrderId(orderId); }
    private OrderInfoResponse fallbackGetByOrderId(Long orderId, Throwable t) {
        log.warn("order-service unavailable for orderId={}: {}", orderId, t.getMessage());
        throw new OrderServiceUnavailableException("Order service is unavailable, cannot fetch order by id: " + orderId);
    }
}
