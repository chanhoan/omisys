package com.omisys.payment.server.domain.repository;

import com.omisys.payment.server.domain.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long>, PaymentRepositoryCustom {

    Optional<Payment> findByPaymentKey(String paymentKey);

    Optional<Payment> findByOrderId(Long orderId);

    Optional<Payment> findByPaymentId(Long paymentId);

}
