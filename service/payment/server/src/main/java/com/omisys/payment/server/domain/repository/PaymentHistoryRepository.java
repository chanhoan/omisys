package com.omisys.payment.server.domain.repository;

import com.omisys.payment.server.domain.model.PaymentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, Long> {

    List<PaymentHistory> findByPayment_PaymentId(Long paymentId);

}
