package com.innowise.paymentservice.repository;

import com.innowise.paymentservice.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface PaymentRepository extends JpaRepository <Payment, Long>, JpaSpecificationExecutor<Payment> {

    @Query("""
            SELECT COALESCE(SUM(p.paymentAmount), 0)
            FROM Payment p
            WHERE p.userId = :userId
              AND p.timestamp BETWEEN :from AND :to
            """)
    BigDecimal sumByUserIdAndTimestampBetween(@Param("userId") Long userId,
                                              @Param("from") LocalDateTime from,@Param("to") LocalDateTime to);
    @Query("""
            SELECT COALESCE(SUM(p.paymentAmount), 0)
            FROM Payment p
            WHERE p.timestamp BETWEEN :from AND :to
            """)
    BigDecimal sumByTimestampBetween(@Param("from") LocalDateTime from,
                                     @Param("to") LocalDateTime to);
}
