package com.omisys.payment.server.presentation.response;

import com.omisys.payment.server.domain.model.PaymentState;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

public class PaymentHistoryResponse {

    @Getter
    @Setter
    @Builder
    public static class Get {

        private PaymentState type;
        private String cancelReason;
        private Long amount;
        private LocalDateTime createdAt;

    }

}
