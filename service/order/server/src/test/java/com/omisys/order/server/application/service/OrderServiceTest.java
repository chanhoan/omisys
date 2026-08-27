package com.omisys.order.server.application.service;

import com.omisys.order.server.domain.model.Order;
import com.omisys.order.server.domain.model.OrderProduct;
import com.omisys.order.server.domain.model.vo.OrderState;
import com.omisys.order.server.domain.repository.OrderProductRepository;
import com.omisys.order.server.domain.repository.OrderRepository;
import com.omisys.order.server.exception.OrderErrorCode;
import com.omisys.order.server.exception.OrderException;
import com.omisys.order.server.infrastructure.client.PaymentClient;
import com.omisys.order.server.infrastructure.client.ProductClient;
import com.omisys.order.server.infrastructure.client.UserClient;
import com.omisys.user_dto.infrastructure.UserDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private UserClient userClient;
    @Mock private PaymentClient paymentClient;
    @Mock private ProductClient productClient;
    @Mock private OrderProductRepository orderProductRepository;

    @InjectMocks private OrderService orderService;

    @Test
    @DisplayName("cancelOrder: 주문이 COMPLETED면 결제취소 + 재고롤백 + 상태전이 수행")
    void cancelOrder_completed_triggers_payment_cancel() {
        // given
        long userId = 1L;
        long orderId = 10L;

        UserDto user = mock(UserDto.class);
        when(userClient.getUser(userId)).thenReturn(user);

        Order order = mock(Order.class);
        when(order.getState()).thenReturn(OrderState.COMPLETED);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        OrderProduct op = mock(OrderProduct.class);
        when(op.getProductId()).thenReturn("p1");
        when(op.getQuantity()).thenReturn(2);
        when(orderProductRepository.findByOrder(order)).thenReturn(List.of(op));

        // when
        Long result = orderService.cancelOrder(userId, orderId);

        // then
        assertThat(result).isEqualTo(orderId);

        verify(userClient).createPointHistory(any());
        verify(productClient).rollbackStock(Map.of("p1", 2));
        verify(paymentClient).cancelPayment(any());

        verify(order).cancel();
    }

    @Test
    @DisplayName("cancelOrder: COMPLETED가 아니면 결제취소는 호출되지 않는다")
    void cancelOrder_not_completed_does_not_cancel_payment() {
        // given
        long userId = 1L;
        long orderId = 10L;

        UserDto user = mock(UserDto.class);
        when(userClient.getUser(userId)).thenReturn(user);

        Order order = mock(Order.class);
//        when(order.getOrderId()).thenReturn(orderId);
//        when(order.getUserId()).thenReturn(userId);
        when(order.getState()).thenReturn(OrderState.PENDING_PAYMENT);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        OrderProduct op = mock(OrderProduct.class);
        when(op.getProductId()).thenReturn("p1");
        when(op.getQuantity()).thenReturn(1);
        when(orderProductRepository.findByOrder(order)).thenReturn(List.of(op));

        // when
        orderService.cancelOrder(userId, orderId);

        // then
        verify(paymentClient, never()).cancelPayment(any());
        verify(productClient).rollbackStock(anyMap());

        // 상태 전이는 동일하게 발생
        verify(order).cancel();
    }

    @Test
    @DisplayName("getAllOrder: state 파라미터 전달 시 OrderState로 변환되어 repository에 전달된다")
    void getAllOrder_withState_convertsToOrderState() {
        // given
        long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        when(userClient.getUser(userId)).thenReturn(mock(UserDto.class));
        when(orderRepository.getAllOrder(eq(pageable), isNull(), isNull(), eq(OrderState.SHIPPING), isNull(), isNull()))
                .thenReturn(new PageImpl<>(List.of()));

        // when
        orderService.getAllOrder(pageable, userId, null, null, "SHIPPING", null, null);

        // then
        verify(orderRepository).getAllOrder(pageable, null, null, OrderState.SHIPPING, null, null);
    }

    @Test
    @DisplayName("getAllOrder: 잘못된 state 문자열이면 ORDER_STATE_NOT_FOUND 예외")
    void getAllOrder_invalidState_throws() {
        // given
        long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        when(userClient.getUser(userId)).thenReturn(mock(UserDto.class));

        // when & then
        assertThatThrownBy(() -> orderService.getAllOrder(pageable, userId, null, null, "XXX", null, null))
                .isInstanceOf(OrderException.class)
                .satisfies(ex -> {
                    OrderException oe = (OrderException) ex;
                    assertThat(oe.getErrorCode()).isEqualTo(OrderErrorCode.ORDER_STATE_NOT_FOUND);
                });

        verify(orderRepository, never()).getAllOrder(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("getAllOrder: 파라미터 없이 호출하면 기존과 동일하게 모두 null로 전달된다(하위호환)")
    void getAllOrder_noParams_backwardCompatible() {
        // given
        long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        when(userClient.getUser(userId)).thenReturn(mock(UserDto.class));
        when(orderRepository.getAllOrder(eq(pageable), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(new PageImpl<>(List.of()));

        // when
        orderService.getAllOrder(pageable, userId, null, null, null, null, null);

        // then
        verify(orderRepository).getAllOrder(pageable, null, null, null, null, null);
    }
}
