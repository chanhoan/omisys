package com.omisys.delivery.server.application.service;

import com.omisys.delivery.server.domain.model.Delivery;
import com.omisys.delivery.server.domain.repository.DeliveryRepository;
import com.omisys.delivery.server.exception.DeliveryException;
import com.omisys.delivery.server.infrastructure.event.PaymentCompletedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryCreateServiceTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @InjectMocks
    private DeliveryCreateService deliveryCreateService;

    @Test
    @DisplayName("결제 성공 이벤트 수신 시 배송 생성")
    void createDelivery_paymentSuccess_createsDelivery() {
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .orderId(1L).userId(10L).paymentId(100L).amount(50000L).success(true)
                .build();

        Delivery mockDelivery = mock(Delivery.class);
        when(mockDelivery.getDeliveryId()).thenReturn(1L);
        when(mockDelivery.getOrderId()).thenReturn(1L);
        when(mockDelivery.getUserId()).thenReturn(10L);
        when(deliveryRepository.existsByOrderId(1L)).thenReturn(false);
        when(deliveryRepository.save(any())).thenReturn(mockDelivery);

        Long result = deliveryCreateService.createDelivery(event);

        verify(deliveryRepository).save(any(Delivery.class));
    }

    @Test
    @DisplayName("결제 실패 이벤트는 배송 생성 건너뜀")
    void createDelivery_paymentFailed_skipsCreation() {
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .orderId(2L).userId(10L).success(false)
                .build();

        Long result = deliveryCreateService.createDelivery(event);

        verify(deliveryRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 배송이 존재하는 주문 — 멱등 처리로 null 반환")
    void createDelivery_alreadyExists_returnsNull() {
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .orderId(3L).userId(10L).success(true)
                .build();

        when(deliveryRepository.existsByOrderId(3L)).thenReturn(true);

        Long result = deliveryCreateService.createDelivery(event);

        assertThat(result).isNull();
        verify(deliveryRepository, never()).save(any());
    }
}
