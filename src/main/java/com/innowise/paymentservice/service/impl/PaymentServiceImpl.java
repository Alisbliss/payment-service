package com.innowise.paymentservice.service.impl;

import com.innowise.paymentservice.client.OrderServiceClient;
import com.innowise.paymentservice.client.RandomNumberService;
import com.innowise.paymentservice.dto.OrderInfoResponse;
import com.innowise.paymentservice.dto.PaymentCreateRequest;
import com.innowise.paymentservice.dto.PaymentResponse;
import com.innowise.paymentservice.entity.Payment;
import com.innowise.paymentservice.entity.Status;
import com.innowise.paymentservice.exception.PaymentNotFoundException;
import com.innowise.paymentservice.mapper.PaymentMapper;
import com.innowise.paymentservice.repository.PaymentRepository;
import com.innowise.paymentservice.repository.specification.PaymentSpecification;
import com.innowise.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service("paymentService")
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final OrderServiceClient orderServiceClient;
    private final RandomNumberService randomNumberService;

    @Override
    @Transactional
    public PaymentResponse create(PaymentCreateRequest request) {
        OrderInfoResponse order = orderServiceClient.getByOrderId(request.getOrderId());
        int random = randomNumberService.getNumber();
        Status status = (random % 2 == 0) ? Status.SUCCESS : Status.FAILED;

        Payment payment = paymentMapper.toEntity(request);
        payment.setOrderId(order.getId());
        payment.setUserId(order.getUserId());
        payment.setStatus(status);
        payment.setPaymentAmount(order.getTotalPrice());

        Payment saved = paymentRepository.save(payment);
        return toResponse(saved, order);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + id));
        OrderInfoResponse order = orderServiceClient.getByOrderId(payment.getOrderId());
        return toResponse(payment, order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getAll(Long userId, Long orderId, Status status, Pageable pageable) {
        return paymentRepository.findAll(PaymentSpecification.filter(userId, orderId, status), pageable)
                .map(payment -> toResponse(payment, orderServiceClient.getByOrderId(payment.getOrderId())));
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalForCurrentUser(Long userId, LocalDateTime from, LocalDateTime to) {
        return paymentRepository.sumByUserIdAndTimestampBetween(userId, from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalForAllUsers(LocalDateTime from, LocalDateTime to) {
        return paymentRepository.sumByTimestampBetween(from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isOrderOwner(Long orderId, Long currentUserId) {
        if (orderId == null || currentUserId == null) {
            return false;
        }
        OrderInfoResponse order = orderServiceClient.getByOrderId(orderId);
        return currentUserId.equals(order.getUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPaymentOwner(Long paymentId, Long currentUserId) {
        if (paymentId == null || currentUserId == null) {
            return false;
        }
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));
        return currentUserId.equals(payment.getUserId());
    }

    private PaymentResponse toResponse(Payment payment, OrderInfoResponse order) {
        PaymentResponse response = paymentMapper.toResponse(payment);
        response.setOrder(order);
        return response;
    }
}