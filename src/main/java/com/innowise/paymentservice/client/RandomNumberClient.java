package com.innowise.paymentservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "random-api", url = "${random-api.url}")
public interface RandomNumberClient {
    @GetMapping("/random")
    Integer getRandomNumber();
}
