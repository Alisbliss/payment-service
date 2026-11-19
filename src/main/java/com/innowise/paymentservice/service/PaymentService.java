package com.innowise.paymentservice.service;

import com.innowise.paymentservice.dto.PaymentCreateRequest;
import com.innowise.paymentservice.dto.PaymentResponse;
import com.innowise.paymentservice.entity.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface PaymentService {
    PaymentResponse create(PaymentCreateRequest request);
    PaymentResponse getById(Long id);
    Page<PaymentResponse> getAll(Long userId, Long orderId, Status status, Pageable pageable);
    BigDecimal getTotalForCurrentUser(Long userId, LocalDateTime from, LocalDateTime to);
    BigDecimal getTotalForAllUsers(LocalDateTime from, LocalDateTime to);
    boolean isOrderOwner(Long orderId, Long currentUserId);
    boolean isPaymentOwner(Long paymentId, Long currentUserId);
}
