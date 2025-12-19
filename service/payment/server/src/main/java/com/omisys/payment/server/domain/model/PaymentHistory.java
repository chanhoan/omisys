package com.omisys.payment.server.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "p_payment_history")
public class PaymentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_history_id")
    private Long paymentHistoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(name = "amount")
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private PaymentState type;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public static PaymentHistory create(Payment payment) {
        PaymentHistory paymentHistory = new PaymentHistory();
        paymentHistory.setPayment(payment);
        paymentHistory.setAmount(payment.getAmount());
        paymentHistory.setType(PaymentState.PAYMENT);
        paymentHistory.setCreatedAt(LocalDateTime.now());
        return paymentHistory;
    }

    public static PaymentHistory cancel(Payment payment, String cancelReason) {
        PaymentHistory paymentHistory = new PaymentHistory();
        paymentHistory.setPayment(payment);
        paymentHistory.setAmount(payment.getAmount());
        paymentHistory.setType(PaymentState.CANCEL);
        paymentHistory.setCancelReason(cancelReason);
        paymentHistory.setCreatedAt(LocalDateTime.now());
        return paymentHistory;
    }


}
