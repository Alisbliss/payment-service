package com.innowise.paymentservice.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final KafkaTemplate<Object, Object> kafkaTemplate;

    @Value("${spring.kafka.topic.create-payment:create-payment}")
    private String createPaymentTopic;

    public void send(PaymentCreatedEvent event) {
        kafkaTemplate.send(createPaymentTopic, String.valueOf(event.getOrderId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish CREATE_PAYMENT event for order {}",
                                event.getOrderId(), ex);
                    } else {
                        log.info("Published CREATE_PAYMENT event for order {} with status {}",
                                event.getOrderId(), event.getStatus());
                    }
                });
    }
}