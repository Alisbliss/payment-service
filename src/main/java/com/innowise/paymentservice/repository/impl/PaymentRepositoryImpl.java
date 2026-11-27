package com.innowise.paymentservice.repository.impl;

import com.innowise.paymentservice.entity.Payment;
import com.innowise.paymentservice.entity.Status;
import com.innowise.paymentservice.repository.PaymentRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepositoryCustom {

    private static final String COLLECTION = "payments";

    private final MongoTemplate mongoTemplate;

    @Override
    public Page<Payment> findPayments(Long userId, Long orderId, Status status, Pageable pageable) {
        Criteria criteria = buildFilter(userId, orderId, status);
        Query contentQuery = new Query(criteria).with(pageable);
        long total = mongoTemplate.count(new Query(criteria), Payment.class);
        List<Payment> content = mongoTemplate.find(contentQuery, Payment.class);
        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public BigDecimal sumForUser(Long userId, LocalDateTime from, LocalDateTime to) {
        Criteria criteria = Criteria.where("user_id").is(userId)
                .and("payment_timestamp").gte(from).lte(to);
        return aggregateSum(criteria);
    }

    @Override
    public BigDecimal sumAll(LocalDateTime from, LocalDateTime to) {
        Criteria criteria = Criteria.where("payment_timestamp").gte(from).lte(to);
        return aggregateSum(criteria);
    }

    private Criteria buildFilter(Long userId, Long orderId, Status status) {
        List<Criteria> filters = new ArrayList<>();
        if (userId != null) filters.add(Criteria.where("user_id").is(userId));
        if (orderId != null) filters.add(Criteria.where("order_id").is(orderId));
        if (status != null) filters.add(Criteria.where("status").is(status.name()));
        Criteria criteria = new Criteria();
        if (!filters.isEmpty()) criteria.andOperator(filters);
        return criteria;
    }

    private BigDecimal aggregateSum(Criteria criteria) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.group().sum("payment_amount").as("total"));
        AggregationResults<Document> results = mongoTemplate.aggregate(aggregation, COLLECTION, Document.class);
        Document document = results.getUniqueMappedResult();
        return document == null ? BigDecimal.ZERO : toBigDecimal(document.get("total"));
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof Decimal128 decimal128) return decimal128.bigDecimalValue();
        if (value instanceof BigDecimal bigDecimal) return bigDecimal;
        if (value instanceof Number number) return new BigDecimal(number.toString());
        return new BigDecimal(value.toString());
    }
}