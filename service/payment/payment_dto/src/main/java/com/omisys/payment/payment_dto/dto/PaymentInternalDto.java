package com.omisys.payment.payment_dto.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

public class PaymentInternalDto {

    @Getter
    @Setter
    @AllArgsConstructor
    public static class Create {

        private Long userId;
        private Long orderId;
        private String orderName;
        private String email;
        private Long amount;

    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class Cancel {

        private Long orderId;
        private String cancelReason;

    }

    @Getter
    @Setter
    public static class Get {

        private Long paymentId;
        private Long orderId;
        private PaymentState state;
        private String orderName;
        private Long amount;
        private LocalDateTime createdAt;
        private List<PaymentHistoryDto> histories;

    }

    @Getter
    @Setter
    public static class PaymentHistoryDto {

        private Long amount;
        private PaymentState type;
        private String cancelReason;
        private LocalDateTime createdAt;

    }

    public enum PaymentState {
        PAYMENT, PENDING, FAILED, CANCEL, REFUND_PENDING, REFUND

    }

}
