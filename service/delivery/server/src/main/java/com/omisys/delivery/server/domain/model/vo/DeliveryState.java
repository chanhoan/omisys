package com.omisys.delivery.server.domain.model.vo;

import com.omisys.delivery.server.exception.DeliveryErrorCode;
import com.omisys.delivery.server.exception.DeliveryException;
import lombok.Getter;

@Getter
public enum DeliveryState {

    READY_FOR_SHIPMENT("배송 준비중"),
    SHIPPING("배송중"),
    DELIVERED("배송 완료"),
    CANCELED("배송 취소");

    private final String description;

    DeliveryState(String description) {
        this.description = description;
    }

    public void validateTransitionTo(DeliveryState next) {
        boolean valid = switch (this) {
            case READY_FOR_SHIPMENT -> next == SHIPPING || next == CANCELED;
            case SHIPPING -> next == DELIVERED;
            case DELIVERED, CANCELED -> false;
        };
        if (!valid) {
            throw new DeliveryException(DeliveryErrorCode.INVALID_DELIVERY_STATE_TRANSITION,
                    this.name() + " -> " + next.name());
        }
    }
}
