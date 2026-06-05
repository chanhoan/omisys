package com.omisys.delivery.server.application.service;

import com.omisys.delivery.server.domain.model.vo.DeliveryState;
import com.omisys.delivery.server.exception.DeliveryException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeliveryStateTest {

    @Test
    @DisplayName("READY_FOR_SHIPMENT → SHIPPING 허용")
    void transition_readyToShipping_allowed() {
        assertThatCode(() -> DeliveryState.READY_FOR_SHIPMENT.validateTransitionTo(DeliveryState.SHIPPING))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("READY_FOR_SHIPMENT → CANCELED 허용")
    void transition_readyToCanceled_allowed() {
        assertThatCode(() -> DeliveryState.READY_FOR_SHIPMENT.validateTransitionTo(DeliveryState.CANCELED))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SHIPPING → DELIVERED 허용")
    void transition_shippingToDelivered_allowed() {
        assertThatCode(() -> DeliveryState.SHIPPING.validateTransitionTo(DeliveryState.DELIVERED))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("DELIVERED → SHIPPING 불허 — DeliveryException")
    void transition_deliveredToShipping_throws() {
        assertThatThrownBy(() -> DeliveryState.DELIVERED.validateTransitionTo(DeliveryState.SHIPPING))
                .isInstanceOf(DeliveryException.class);
    }

    @Test
    @DisplayName("CANCELED → SHIPPING 불허 — DeliveryException")
    void transition_canceledToShipping_throws() {
        assertThatThrownBy(() -> DeliveryState.CANCELED.validateTransitionTo(DeliveryState.SHIPPING))
                .isInstanceOf(DeliveryException.class);
    }

    @Test
    @DisplayName("SHIPPING → READY_FOR_SHIPMENT 역방향 불허")
    void transition_shippingToReady_throws() {
        assertThatThrownBy(() -> DeliveryState.SHIPPING.validateTransitionTo(DeliveryState.READY_FOR_SHIPMENT))
                .isInstanceOf(DeliveryException.class);
    }
}
