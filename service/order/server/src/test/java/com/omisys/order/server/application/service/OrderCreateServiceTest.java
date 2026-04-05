package com.omisys.order.server.application.service;

import com.omisys.order.order_dto.dto.OrderCreateRequest;
import com.omisys.order.order_dto.dto.OrderProductInfo;
import com.omisys.order.server.domain.model.Order;
import com.omisys.order.server.domain.model.vo.OrderType;
import com.omisys.order.server.exception.OrderErrorCode;
import com.omisys.order.server.exception.OrderException;
import com.omisys.order.server.infrastructure.client.PaymentClient;
import com.omisys.order.server.infrastructure.client.ProductClient;
import com.omisys.order.server.infrastructure.client.UserClient;
import com.omisys.order.server.domain.repository.OrderProductRepository;
import com.omisys.order.server.domain.repository.OrderRepository;
import com.omisys.payment.payment_dto.dto.PaymentInternalDto;
import com.omisys.product.product_dto.ProductDto;
import com.omisys.user_dto.infrastructure.AddressDto;
import com.omisys.user_dto.infrastructure.UserDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderCreateServiceTest {

    @Mock private UserClient userClient;
    @Mock private CartService cartService;
    @Mock private OrderProductRepository orderProductRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private ProductClient productClient;
    @Mock private PaymentClient paymentClient;
    @Mock private OrderRollbackService orderRollbackService;

    @InjectMocks private OrderCreateService orderCreateService;

    @Test
    @DisplayName("createOrder 성공(STANDARD): 재고차감→주문저장→주문상품저장→장바구니정리→결제생성 흐름이 보장된다")
    void createOrder_success_standard_flow() {
        // given
        long userId = 1L;

        UUID productId = UUID.randomUUID();
        String productIdStr = productId.toString();

        OrderCreateRequest request = new OrderCreateRequest(
                OrderType.STANDARD.name(),
                List.of(new OrderProductInfo(productIdStr, 2, null)),
                BigDecimal.ZERO,
                100L
        );

        // ✅ UserDto / AddressDto는 "실객체"로 만들 수 있으면 가장 좋다.
        // 레퍼런스 DTO가 생성자가 막혀있을 수 있어서, 여기서는 mock을 쓰되 "실제로 쓰는 값"만 최소 stubbing 한다.
        UserDto user = mock(UserDto.class);
//        when(user.getPoint()).thenReturn(BigDecimal.ZERO); // 포인트 검증에 사용될 수 있음
        when(userClient.getUser(userId)).thenReturn(user);

        AddressDto address = mock(AddressDto.class);
        when(address.getUserId()).thenReturn(userId); // 주소 검증에 사용
        when(userClient.getAddress(100L)).thenReturn(address);

        // ✅ ProductDto는 createOrder 내부 price 계산에서 discountedPrice가 반드시 필요함
        ProductDto product = mock(ProductDto.class);
        when(product.getProductId()).thenReturn(productId);
        when(product.getStock()).thenReturn(10);
        when(product.getDiscountedPrice()).thenReturn(BigDecimal.valueOf(5000)); // Order.calculatePrice에서 사용됨
        when(productClient.getProductList(List.of(productIdStr))).thenReturn(List.of(product));

        when(orderRepository.existsByOrderNo(anyString())).thenReturn(false);

        // save 시 orderId를 강제로 심어준다 (영속화 없이 단위 테스트로 id 흐름 검증)
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0, Order.class);
            ReflectionTestUtils.setField(saved, "orderId", 999L);
            return saved;
        });

        // when
        Long orderId = orderCreateService.createOrder(userId, request);

        // then
        assertThat(orderId).isEqualTo(999L);

        // ✅ 호출 순서(오케스트레이션) 검증
        InOrder inOrder = inOrder(userClient, productClient, orderRepository, orderProductRepository, cartService, paymentClient);

        inOrder.verify(userClient).getUser(userId);
        inOrder.verify(userClient).getAddress(100L);

        inOrder.verify(productClient).getProductList(List.of(productIdStr));
        inOrder.verify(productClient).updateStock(Map.of(productIdStr, 2));

        inOrder.verify(orderRepository).save(any(Order.class));
        inOrder.verify(orderProductRepository).save(any());

        inOrder.verify(cartService).orderCartProduct(userId, Map.of(productIdStr, 2));
        inOrder.verify(paymentClient).payment(any(PaymentInternalDto.Create.class));

        verify(orderRollbackService, never()).rollbackTransaction(anyMap(), anyList(), any());
    }

    @Test
    @DisplayName("createOrder 성공(PREORDER): cartService.orderCartProduct는 호출되지 않는다")
    void createOrder_success_preorder_no_cart_cleanup() {
        // given
        long userId = 1L;

        UUID productId = UUID.randomUUID();
        String productIdStr = productId.toString();

        OrderCreateRequest request = new OrderCreateRequest(
                OrderType.PREORDER.name(),
                List.of(new OrderProductInfo(productIdStr, 1, null)),
                BigDecimal.ZERO,
                100L
        );

        UserDto user = mock(UserDto.class);
//        when(user.getPoint()).thenReturn(BigDecimal.ZERO);
        when(userClient.getUser(userId)).thenReturn(user);

        AddressDto address = mock(AddressDto.class);
        when(address.getUserId()).thenReturn(userId);
        when(userClient.getAddress(100L)).thenReturn(address);

        ProductDto product = mock(ProductDto.class);
        when(product.getProductId()).thenReturn(productId);
        when(product.getStock()).thenReturn(10);
        when(product.getDiscountedPrice()).thenReturn(BigDecimal.valueOf(10000));
        when(productClient.getProductList(List.of(productIdStr))).thenReturn(List.of(product));

        when(orderRepository.existsByOrderNo(anyString())).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0, Order.class);
            ReflectionTestUtils.setField(saved, "orderId", 1000L);
            return saved;
        });

        // when
        Long orderId = orderCreateService.createOrder(userId, request);

        // then
        assertThat(orderId).isEqualTo(1000L);

        verify(cartService, never()).orderCartProduct(anyLong(), anyMap());
        verify(paymentClient).payment(any(PaymentInternalDto.Create.class));

        verify(orderRollbackService, never()).rollbackTransaction(anyMap(), anyList(), any());
    }

    @Test
    @DisplayName("createOrder 실패: 주소 주인이 다르면 ADDRESS_MISMATCH 예외 + 이후 흐름 중단")
    void createOrder_fail_address_mismatch() {
        // given
        long userId = 1L;

        UUID productId = UUID.randomUUID();
        String productIdStr = productId.toString();

        OrderCreateRequest request = new OrderCreateRequest(
                OrderType.STANDARD.name(),
                List.of(new OrderProductInfo(productIdStr, 1, null)),
                BigDecimal.ZERO,
                100L
        );

        UserDto user = mock(UserDto.class);
        when(userClient.getUser(userId)).thenReturn(user);

        AddressDto address = mock(AddressDto.class);
        when(address.getUserId()).thenReturn(999L); // mismatch
        when(address.getAddressId()).thenReturn(100L);
        when(userClient.getAddress(100L)).thenReturn(address);

        // when & then
        assertThatThrownBy(() -> orderCreateService.createOrder(userId, request))
                .isInstanceOf(OrderException.class)
                .satisfies(ex -> {
                    OrderException oe = (OrderException) ex;
                    assertThat(oe.getMessage())
                            .isEqualTo(String.format(OrderErrorCode.ADDRESS_MISMATCH.getMessage(), 100L));
                });

        verify(productClient, never()).getProductList(anyList());
        verify(productClient, never()).updateStock(anyMap());
        verify(orderRepository, never()).save(any());
        verify(paymentClient, never()).payment(any());

        // 현재 구현이 catch에서 rollbackTransaction 호출한다면 여기서 호출되는 것이 정상
        verify(orderRollbackService).rollbackTransaction(anyMap(), anyList(), any());
    }

    @Test
    @DisplayName("createOrder 실패: 재고 부족이면 INSUFFICIENT_STOCK 예외 + rollbackTransaction 호출")
    void createOrder_fail_insufficient_stock() {
        // given
        long userId = 1L;

        UUID productId = UUID.randomUUID();
        String productIdStr = productId.toString();

        OrderCreateRequest request = new OrderCreateRequest(
                OrderType.STANDARD.name(),
                List.of(new OrderProductInfo(productIdStr, 5, null)),
                BigDecimal.ZERO,
                100L
        );

        UserDto user = mock(UserDto.class);
        when(userClient.getUser(userId)).thenReturn(user);

        AddressDto address = mock(AddressDto.class);
        when(address.getUserId()).thenReturn(userId);
        when(userClient.getAddress(100L)).thenReturn(address);

        ProductDto product = mock(ProductDto.class);
        when(product.getProductId()).thenReturn(productId);
        when(product.getStock()).thenReturn(1); // 부족
//        when(product.getDiscountedPrice()).thenReturn(BigDecimal.valueOf(5000));

        when(productClient.getProductList(List.of(productIdStr))).thenReturn(List.of(product));

        // when & then
        assertThatThrownBy(() -> orderCreateService.createOrder(userId, request))
                .isInstanceOf(OrderException.class)
                .satisfies(ex -> {
                    OrderException oe = (OrderException) ex;
                    assertThat(oe.getMessage())
                            .isEqualTo(String.format(OrderErrorCode.INSUFFICIENT_STOCK.getMessage(), productId));
                });

        verify(productClient, never()).updateStock(anyMap());
        verify(paymentClient, never()).payment(any());

        verify(orderRollbackService).rollbackTransaction(anyMap(), anyList(), any());
    }

    @Test
    @DisplayName("createOrder 실패: 포인트 부족이면 INSUFFICIENT_POINT 예외 + rollbackTransaction 호출")
    void createOrder_fail_insufficient_point() {
        // given
        long userId = 1L;

        UUID productId = UUID.randomUUID();
        String productIdStr = productId.toString();

        OrderCreateRequest request = new OrderCreateRequest(
                OrderType.STANDARD.name(),
                List.of(new OrderProductInfo(productIdStr, 1, null)),
                BigDecimal.valueOf(1000), // 사용 포인트
                100L
        );

        UserDto user = mock(UserDto.class);
        when(user.getPoint()).thenReturn(BigDecimal.valueOf(100)); // 부족
        when(userClient.getUser(userId)).thenReturn(user);

        AddressDto address = mock(AddressDto.class);
        when(address.getUserId()).thenReturn(userId);
        when(userClient.getAddress(100L)).thenReturn(address);

        ProductDto product = mock(ProductDto.class);
        when(product.getProductId()).thenReturn(productId);
        when(product.getStock()).thenReturn(10);
        when(product.getDiscountedPrice()).thenReturn(BigDecimal.valueOf(10000));
        when(productClient.getProductList(List.of(productIdStr))).thenReturn(List.of(product));

        when(orderRepository.existsByOrderNo(anyString())).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0, Order.class);
            ReflectionTestUtils.setField(saved, "orderId", 888L);
            return saved;
        });

        // when & then
        assertThatThrownBy(() -> orderCreateService.createOrder(userId, request))
                .isInstanceOf(OrderException.class)
                .satisfies(ex -> {
                    OrderException oe = (OrderException) ex;
                    assertThat(oe.getStatusName()).isEqualTo(OrderErrorCode.INSUFFICIENT_POINT.getStatus().name());
                    assertThat(oe.getMessage()).isEqualTo(OrderErrorCode.INSUFFICIENT_POINT.getMessage());
                });

        verify(paymentClient, never()).payment(any());
        verify(orderRollbackService).rollbackTransaction(anyMap(), anyList(), any());
    }
}
