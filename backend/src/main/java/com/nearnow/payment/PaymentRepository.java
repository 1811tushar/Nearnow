package com.nearnow.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPaymentReference(String paymentReference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.paymentReference = :reference")
    Optional<Payment> findByPaymentReferenceForUpdate(@Param("reference") String reference);

    Optional<Payment> findByOrderId(Long orderId);

    Optional<Payment> findFirstByUserIdAndStatusOrderByCreatedAtDesc(Long userId, PaymentStatus status);
}
