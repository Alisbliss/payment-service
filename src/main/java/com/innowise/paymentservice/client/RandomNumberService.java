package com.innowise.paymentservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
@RequiredArgsConstructor
public class RandomNumberService {
    private static final Logger log = LoggerFactory.getLogger(RandomNumberService.class);
    private final RandomNumberClient client;

    @CircuitBreaker(name = "randomApi", fallbackMethod = "fallback")
    public int getNumber() {
        return client.getRandomNumber();
    }

    private int fallback(Throwable t) {
        log.warn("random-api unavailable: {}", t.getMessage());
        return new Random().nextInt(100);
    }
}
