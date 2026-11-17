package com.innowise.paymentservice.repository.specification;

import com.innowise.paymentservice.entity.Payment;
import com.innowise.paymentservice.entity.Status;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import jakarta.persistence.criteria.Predicate;

public final class PaymentSpecification {
    private PaymentSpecification() {}

    public static Specification<Payment> filter(Long userId, Long orderId, Status status) {
        return (root, criteriaQuery, cb) -> {
            ArrayList<Predicate> predicates = new ArrayList<>();
            if (userId != null) predicates.add(cb.equal(root.get("userId"), userId));
            if (orderId != null) predicates.add(cb.equal(root.get("orderId"), orderId));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
