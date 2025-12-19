package com.omisys.payment.server.presentation.response;

import com.omisys.payment.server.domain.model.PaymentState;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

public class PaymentResponse {

    @Getter
    @Setter
    @Builder
    public static class Create {

        private String paymentKey;
        private Long orderId;
        private String orderName;
        private String status;
        private String requestedAt;
        private Long totalAmount;
        private Object checkout;

    }

    @Getter
    @Setter
    @Builder
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
    @Builder
    public static class PaymentHistoryDto {

        private Long amount;
        private PaymentState type;
        private String cancelReason;
        private LocalDateTime createdAt;

    }

}
