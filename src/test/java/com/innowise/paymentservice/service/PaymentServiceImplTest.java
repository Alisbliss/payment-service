package com.innowise.paymentservice.service;

import com.innowise.paymentservice.client.OrderServiceClient;
import com.innowise.paymentservice.client.RandomNumberService;
import com.innowise.paymentservice.dto.OrderInfoResponse;
import com.innowise.paymentservice.dto.PaymentCreateRequest;
import com.innowise.paymentservice.dto.PaymentResponse;
import com.innowise.paymentservice.entity.Payment;
import com.innowise.paymentservice.entity.Status;
import com.innowise.paymentservice.exception.OrderServiceUnavailableException;
import com.innowise.paymentservice.exception.PaymentNotFoundException;
import com.innowise.paymentservice.mapper.PaymentMapper;
import com.innowise.paymentservice.repository.PaymentRepository;
import com.innowise.paymentservice.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.innowise.paymentservice.event.PaymentEventProducer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock private PaymentMapper paymentMapper;
    @Mock private OrderServiceClient orderServiceClient;
    @Mock private RandomNumberService randomNumberService;
    @Mock private PaymentEventProducer paymentEventProducer;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private PaymentCreateRequest createRequest() {
        return PaymentCreateRequest.builder().orderId(1L).build();
    }

    private OrderInfoResponse orderInfo(Long orderId, Long userId, String totalPrice) {
        return OrderInfoResponse.builder()
                .id(orderId)
                .userId(userId)
                .totalPrice(new BigDecimal(totalPrice))
                .build();
    }

    private Payment payment(Long id, Long userId, Status status, String amount) {
        Payment p = new Payment();
        p.setId(String.valueOf(id));
        p.setOrderId(1L);
        p.setUserId(userId);
        p.setStatus(status);
        p.setPaymentAmount(new BigDecimal(amount));
        p.setTimestamp(LocalDateTime.now());
        return p;
    }

    //create
    @Test
    void create_shouldSetSuccessStatus_whenRandomIsEven() {
        PaymentCreateRequest req = createRequest();
        OrderInfoResponse order = orderInfo(1L, 5L, "100.00");
        Payment mapped = new Payment();

        when(orderServiceClient.getByOrderId(1L)).thenReturn(order);
        when(randomNumberService.getNumber()).thenReturn(42);
        when(paymentMapper.toEntity(req)).thenReturn(mapped);
        when(paymentRepository.save(mapped)).thenReturn(mapped);
        when(paymentMapper.toResponse(mapped)).thenReturn(new PaymentResponse());
        paymentService.create(req);

        assertThat(mapped.getStatus()).isEqualTo(Status.SUCCESS);
        assertThat(mapped.getUserId()).isEqualTo(5L);
        assertThat(mapped.getOrderId()).isEqualTo(1L);
        assertThat(mapped.getPaymentAmount()).isEqualByComparingTo("100.00");
        verify(paymentRepository).save(mapped);
    }

    @Test
    void create_shouldSetFailedStatus_whenRandomIsOdd() {
        PaymentCreateRequest req = createRequest();
        OrderInfoResponse order = orderInfo(1L, 5L, "100.00");
        Payment mapped = new Payment();

        when(orderServiceClient.getByOrderId(1L)).thenReturn(order);
        when(randomNumberService.getNumber()).thenReturn(7);
        when(paymentMapper.toEntity(req)).thenReturn(mapped);
        when(paymentRepository.save(mapped)).thenReturn(mapped);
        when(paymentMapper.toResponse(mapped)).thenReturn(new PaymentResponse());
        paymentService.create(req);

        assertThat(mapped.getStatus()).isEqualTo(Status.FAILED);
    }

    @Test
    void create_shouldPropagateOrderServiceUnavailable() {
        PaymentCreateRequest req = createRequest();
        when(orderServiceClient.getByOrderId(1L))
                .thenThrow(new OrderServiceUnavailableException("down"));

        assertThatThrownBy(() -> paymentService.create(req))
                .isInstanceOf(OrderServiceUnavailableException.class);
        verify(paymentRepository, never()).save(any());
    }

    //getById
    @Test
    void getById_shouldReturnPaymentWithOrder() {
        Payment p = payment(1L, 5L, Status.SUCCESS, "100.00");
        OrderInfoResponse order = orderInfo(1L, 5L, "100.00");
        PaymentResponse response = new PaymentResponse();

        when(paymentRepository.findById("1")).thenReturn(Optional.of(p));
        when(orderServiceClient.getByOrderId(1L)).thenReturn(order);
        when(paymentMapper.toResponse(p)).thenReturn(response);
        PaymentResponse result = paymentService.getById("1");

        assertThat(result).isSameAs(response);
        assertThat(response.getOrder()).isEqualTo(order);
    }

    @Test
    void getById_shouldThrowWhenNotFound() {
        when(paymentRepository.findById("99")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getById("99"))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessageContaining("Payment not found: 99");
    }

    //getAll
    @Test
    void getAll_shouldReturnPageWithOrderInfo() {
        Pageable pageable = PageRequest.of(0, 10);
        Payment p = payment(1L, 5L, Status.SUCCESS, "100.00");
        Page<Payment> page = new PageImpl<>(List.of(p), pageable, 1);
        OrderInfoResponse order = orderInfo(1L, 5L, "100.00");
        PaymentResponse response = new PaymentResponse();

        when(paymentRepository.findPayments(5L, null, null, pageable)).thenReturn(page);
        when(orderServiceClient.getByOrderId(1L)).thenReturn(order);
        when(paymentMapper.toResponse(p)).thenReturn(response);
        Page<PaymentResponse> result = paymentService.getAll(5L, null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0)).isSameAs(response);
    }

    //totals
    @Test
    void getTotalForCurrentUser_shouldDelegateToRepository() {
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now().plusDays(1);
        when(paymentRepository.sumForUser(5L, from, to))
                .thenReturn(new BigDecimal("150.00"));
        BigDecimal total = paymentService.getTotalForCurrentUser(5L, from, to);

        assertThat(total).isEqualByComparingTo("150.00");
    }

    @Test
    void getTotalForAllUsers_shouldDelegateToRepository() {
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now().plusDays(1);
        when(paymentRepository.sumAll(from, to))
                .thenReturn(new BigDecimal("175.00"));
        BigDecimal total = paymentService.getTotalForAllUsers(from, to);

        assertThat(total).isEqualByComparingTo("175.00");
    }

    //ownership
    @Test
    void isOrderOwner_shouldReturnTrueForOwner() {
        when(orderServiceClient.getByOrderId(1L)).thenReturn(orderInfo(1L, 5L, "100.00"));

        assertThat(paymentService.isOrderOwner(1L, 5L)).isTrue();
    }

    @Test
    void isOrderOwner_shouldReturnFalseForAnotherUser() {
        when(orderServiceClient.getByOrderId(1L)).thenReturn(orderInfo(1L, 5L, "100.00"));

        assertThat(paymentService.isOrderOwner(1L, 99L)).isFalse();
    }

    @Test
    void isOrderOwner_shouldReturnFalseWhenNullArgs() {
        assertThat(paymentService.isOrderOwner(null, 5L)).isFalse();
        assertThat(paymentService.isOrderOwner(1L, null)).isFalse();
    }

    @Test
    void isPaymentOwner_shouldReturnTrueForOwner() {
        when(paymentRepository.findById("1"))
                .thenReturn(Optional.of(payment(1L, 5L, Status.SUCCESS, "100.00")));

        assertThat(paymentService.isPaymentOwner("1", 5L)).isTrue();
    }

    @Test
    void isPaymentOwner_shouldReturnFalseForAnotherUser() {
        when(paymentRepository.findById("1"))
                .thenReturn(Optional.of(payment(1L, 5L, Status.SUCCESS, "100.00")));

        assertThat(paymentService.isPaymentOwner("1", 99L)).isFalse();
    }

    @Test
    void isPaymentOwner_shouldThrowWhenPaymentNotFound() {
        when(paymentRepository.findById("99")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.isPaymentOwner("99", 5L))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessageContaining("Payment not found: 99");
    }

    @Test
    void isPaymentOwner_shouldReturnFalseWhenNullArgs() {
        assertThat(paymentService.isPaymentOwner(null, 5L)).isFalse();
        assertThat(paymentService.isPaymentOwner("1", null)).isFalse();
    }
}
