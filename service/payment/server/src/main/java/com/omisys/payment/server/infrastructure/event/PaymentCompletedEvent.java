package com.omisys.payment.server.infrastructure.event;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCompletedEvent {

    private Long orderId;
    private Long paymentId;
    private Long userId;
    private Long amount;
    private Boolean success;

}
