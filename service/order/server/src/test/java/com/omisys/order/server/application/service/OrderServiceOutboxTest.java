package com.omisys.order.server.application.service;

import com.omisys.common.domain.entity.KafkaTopicConstant;
import com.omisys.order.server.domain.model.Order;
import com.omisys.order.server.domain.model.OrderProduct;
import com.omisys.order.server.domain.model.outbox.OutboxEvent;
import com.omisys.order.server.domain.model.outbox.OutboxStatus;
import com.omisys.order.server.domain.model.vo.OrderState;
import com.omisys.order.server.domain.repository.OrderProductRepository;
import com.omisys.order.server.domain.repository.OrderRepository;
import com.omisys.order.server.infrastructure.client.PaymentClient;
import com.omisys.order.server.infrastructure.client.ProductClient;
import com.omisys.order.server.infrastructure.client.UserClient;
import com.omisys.order.server.infrastructure.repository.OutboxEventRepository;
import com.omisys.user_dto.infrastructure.UserDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceOutboxTest {

    @Mock private OrderRepository orderRepository;
    @Mock private UserClient userClient;
    @Mock private PaymentClient paymentClient;
    @Mock private ProductClient productClient;
    @Mock private OrderProductRepository orderProductRepository;
    @Mock private OutboxEventRepository outboxEventRepository;
    @Mock private OrderCreateService orderCreateService;

    @InjectMocks private OrderService orderService;

    @Test
    @DisplayName("updateOrderState: 알림 대상 상태(COMPLETED)로 변경 시 OutboxEvent 저장")
    void updateOrderState_notifiableState_savesOutboxEvent() {
        long userId = 1L;
        long orderId = 10L;

        UserDto admin = mock(UserDto.class);
        when(admin.getRole()).thenReturn(UserDto.UserRole.ROLE_ADMIN.name());
        when(userClient.getUser(userId)).thenReturn(admin);

        Order order = mock(Order.class);
        when(order.getOrderId()).thenReturn(orderId);
        when(order.getUserId()).thenReturn(userId);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        OrderProduct op = mock(OrderProduct.class);
        when(op.getProductName()).thenReturn("테스트상품");
        when(order.getOrderProducts()).thenReturn(List.of(op));
        when(order.getTotalQuantity()).thenReturn(1);

        orderService.updateOrderState(userId, orderId, "COMPLETED");

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());

        OutboxEvent saved = captor.getValue();
        assertThat(saved.getEventType()).isEqualTo(KafkaTopicConstant.ORDER_STATUS_CHANGED);
        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(saved.getMessageKey()).isEqualTo(String.valueOf(userId));
    }

    @Test
    @DisplayName("updateOrderState: 알림 불필요 상태(READY_FOR_SHIPMENT) 변경 시 OutboxEvent 저장 안 함")
    void updateOrderState_nonNotifiableState_doesNotSaveOutboxEvent() {
        long userId = 1L;
        long orderId = 10L;

        UserDto admin = mock(UserDto.class);
        when(admin.getRole()).thenReturn(UserDto.UserRole.ROLE_ADMIN.name());
        when(userClient.getUser(userId)).thenReturn(admin);

        Order order = mock(Order.class);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        orderService.updateOrderState(userId, orderId, "READY_FOR_SHIPMENT");

        verify(outboxEventRepository, never()).save(any());
    }
}
