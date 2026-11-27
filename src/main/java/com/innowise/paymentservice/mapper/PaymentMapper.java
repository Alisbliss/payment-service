package com.innowise.paymentservice.mapper;

import com.innowise.paymentservice.dto.PaymentCreateRequest;
import com.innowise.paymentservice.dto.PaymentResponse;
import com.innowise.paymentservice.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PaymentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orderId", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "timestamp", ignore = true)
    @Mapping(target = "paymentAmount", ignore = true)
    Payment toEntity(PaymentCreateRequest request);

    @Mapping(target = "order", ignore = true)
    PaymentResponse toResponse(Payment payment);
}


