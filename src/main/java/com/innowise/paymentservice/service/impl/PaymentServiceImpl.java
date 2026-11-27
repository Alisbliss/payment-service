package com.innowise.paymentservice.service.impl;

import com.innowise.paymentservice.client.OrderServiceClient;
import com.innowise.paymentservice.client.RandomNumberService;
import com.innowise.paymentservice.dto.OrderInfoResponse;
import com.innowise.paymentservice.dto.PaymentCreateRequest;
import com.innowise.paymentservice.dto.PaymentResponse;
import com.innowise.paymentservice.entity.Payment;
import com.innowise.paymentservice.entity.Status;
import com.innowise.paymentservice.event.PaymentCreatedEvent;
import com.innowise.paymentservice.event.PaymentEventProducer;
import com.innowise.paymentservice.exception.PaymentNotFoundException;
import com.innowise.paymentservice.mapper.PaymentMapper;
import com.innowise.paymentservice.repository.PaymentRepository;
import com.innowise.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service("paymentService")
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final OrderServiceClient orderServiceClient;
    private final RandomNumberService randomNumberService;
    private final PaymentEventProducer paymentEventProducer;

    @Override
    public PaymentResponse create(PaymentCreateRequest request) {
        OrderInfoResponse order = orderServiceClient.getByOrderId(request.getOrderId());
        int random = randomNumberService.getNumber();
        Status status = (random % 2 == 0) ? Status.SUCCESS : Status.FAILED;

        Payment payment = paymentMapper.toEntity(request);
        payment.setOrderId(order.getId());
        payment.setUserId(order.getUserId());
        payment.setStatus(status);
        payment.setPaymentAmount(order.getTotalPrice());
        payment.setTimestamp(LocalDateTime.now());

        Payment saved = paymentRepository.save(payment);
        publishCreatedEvent(saved);
        return toResponse(saved, order);
    }

    private void publishCreatedEvent(Payment payment) {
        PaymentCreatedEvent event = PaymentCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("CREATE_PAYMENT")
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .status(payment.getStatus())
                .amount(payment.getPaymentAmount())
                .timestamp(payment.getTimestamp())
                .build();
        try {
            paymentEventProducer.send(event);
        } catch (Exception e) {
            log.error("Failed to publish CREATE_PAYMENT event for order {}", payment.getOrderId(), e);
        }
    }

    @Override
    public PaymentResponse getById(String id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + id));
        OrderInfoResponse order = orderServiceClient.getByOrderId(payment.getOrderId());
        return toResponse(payment, order);
    }

    @Override
    public Page<PaymentResponse> getAll(Long userId, Long orderId, Status status, Pageable pageable) {
        return paymentRepository.findPayments(userId, orderId, status, pageable)
                .map(payment -> toResponse(payment, orderServiceClient.getByOrderId(payment.getOrderId())));
    }

    @Override
    public BigDecimal getTotalForCurrentUser(Long userId, LocalDateTime from, LocalDateTime to) {
        return paymentRepository.sumForUser(userId, from, to);
    }

    @Override
    public BigDecimal getTotalForAllUsers(LocalDateTime from, LocalDateTime to) {
        return paymentRepository.sumAll(from, to);
    }

    @Override
    public boolean isOrderOwner(Long orderId, Long currentUserId) {
        if (orderId == null || currentUserId == null) {
            return false;
        }
        OrderInfoResponse order = orderServiceClient.getByOrderId(orderId);
        return currentUserId.equals(order.getUserId());
    }

    @Override
    public boolean isPaymentOwner(String paymentId, Long currentUserId) {
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
