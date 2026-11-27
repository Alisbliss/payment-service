package com.innowise.paymentservice.event;

import com.innowise.paymentservice.entity.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCreatedEvent {
    private String eventId;
    private String eventType;
    private String paymentId;
    private Long orderId;
    private Long userId;
    private Status status;
    private BigDecimal amount;
    private LocalDateTime timestamp;
}