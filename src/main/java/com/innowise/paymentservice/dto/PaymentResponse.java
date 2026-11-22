package com.innowise.paymentservice.dto;

import com.innowise.paymentservice.entity.Status;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private String id;
    private Long orderId;
    private Long userId;
    private Status status;
    private LocalDateTime timestamp;
    private BigDecimal paymentAmount;
    private OrderInfoResponse order;
}
