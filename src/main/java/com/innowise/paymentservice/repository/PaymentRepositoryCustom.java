package com.innowise.paymentservice.repository;

import com.innowise.paymentservice.entity.Payment;
import com.innowise.paymentservice.entity.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface PaymentRepositoryCustom {
    Page<Payment> findPayments(Long userId, Long orderId, Status status, Pageable pageable);
    BigDecimal sumForUser(Long userId, LocalDateTime from, LocalDateTime to);
    BigDecimal sumAll(LocalDateTime from, LocalDateTime to);
}