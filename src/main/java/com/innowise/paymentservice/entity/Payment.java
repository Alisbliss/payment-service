package com.innowise.paymentservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "payments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    @Id
    private String id;

    @Field("user_id")
    private Long userId;

    @Field("order_id")
    private Long orderId;

    @Field("status")
    private Status status;

    @Field("payment_timestamp")
    private LocalDateTime timestamp;

    @Field("payment_amount")
    private BigDecimal paymentAmount;
}
