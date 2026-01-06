package com.omisys.product.application.preorder;

import com.omisys.product.domain.model.PreOrder;
import com.omisys.product.domain.model.PreOrderState;
import com.omisys.product.domain.model.Product;
import com.omisys.product.domain.repository.cassandra.ProductRepository;
import com.omisys.product.domain.repository.jpa.PreOrderRepository;
import com.omisys.product.exception.ProductErrorCode;
import com.omisys.product.exception.ProductException;
import com.omisys.product.infrastructure.utils.PreOrderRedisDto;
import com.omisys.product.presentation.request.PreOrderRequest;
import com.omisys.product.presentation.response.PreOrderResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.omisys.product.infrastructure.utils.RedisUtils.getRedisKeyOfPreOrder;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PreOrderServiceTest {

    @Mock private PreOrderRepository preOrderRepository;
    @Mock private ProductRepository productRepository;
    @Mock private PreOrderRedisService preOrderRedisService;
    @Mock private PreOrderCacheService preOrderCacheService;
    @Mock private DistributedLockComponent distributedLockComponent;
    @InjectMocks private PreOrderLockService preOrderLockService;

    @InjectMocks
    private PreOrderService preOrderService;

    @Test
    @DisplayName("createPreOrder: 재고 검증 통과 → PreOrder 생성/저장")
    void createPreOrder_success() {
        // given
        UUID productId = UUID.randomUUID();

        Product product = Product.builder()
                .categoryId(1L)
                .productName("p")
                .brandName("b")
                .mainColor("c")
                .size("s")
                .originalPrice(BigDecimal.valueOf(1000))
                .discountPercent(null)
                .stock(10) // 현재 재고
                .description("d")
                .originImgUrl("o")
                .detailImgUrl("d")
                .thumbnailImgUrl("t")
                .limitCountPerUser(1)
                .tags(List.of("tag"))
                .build();

        when(productRepository.findByProductIdAndIsDeletedFalse(productId)).thenReturn(Optional.of(product));

        PreOrderRequest.Create request = new PreOrderRequest.Create(
                productId,
                "pre-title",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(10),
                5 // 요청 수량
        );

        // when
        preOrderService.createPreOrder(request);

        // then
        ArgumentCaptor<PreOrder> captor = ArgumentCaptor.forClass(PreOrder.class);
        verify(preOrderRepository).save(captor.capture());

        PreOrder saved = captor.getValue();
        assertThat(saved.getProductId()).isEqualTo(productId);
        assertThat(saved.getPreOrderTitle()).isEqualTo("pre-title");
        assertThat(saved.getAvailableQuantity()).isEqualTo(5);
        assertThat(saved.getState()).isEqualTo(PreOrderState.INITIALIZED);
    }

    @Test
    @DisplayName("createPreOrder: 재고 부족(현재재고 <= 요청수량) → PREORDER_QUANTITY_CONFLICT 예외")
    void createPreOrder_insufficientStock_throws() {
        // given
        UUID productId = UUID.randomUUID();

        Product product = Product.builder()
                .categoryId(1L)
                .productName("p")
                .brandName("b")
                .mainColor("c")
                .size("s")
                .originalPrice(BigDecimal.valueOf(1000))
                .discountPercent(null)
                .stock(5) // 현재 재고
                .description("d")
                .originImgUrl("o")
                .detailImgUrl("d")
                .thumbnailImgUrl("t")
                .limitCountPerUser(1)
                .tags(List.of("tag"))
                .build();

        when(productRepository.findByProductIdAndIsDeletedFalse(productId)).thenReturn(Optional.of(product));

        PreOrderRequest.Create request = new PreOrderRequest.Create(
                productId,
                "pre-title",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(10),
                5 // 요청 수량 (코드상 nowQuantity <= requestQuantity 면 conflict)
        );

        // when & then
        assertThatThrownBy(() -> preOrderService.createPreOrder(request))
                .isInstanceOf(ProductException.class)
                .extracting(ex -> ((ProductException) ex).getErrorCode())
                .isEqualTo(ProductErrorCode.PREORDER_QUANTITY_CONFLICT);

        verify(preOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateState: OPEN_FOR_ORDER 요청 → PreOrder.open() 수행")
    void updateState_open_success() {
        // given
        long preOrderId = 1L;

        PreOrder preOrder = PreOrder.create(new PreOrderRequest.Create(
                UUID.randomUUID(),
                "title",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(10),
                3
        ));
        // JPA ID는 private 필드라 테스트에서 reflection으로 세팅
        ReflectionTestUtils.setField(preOrder, "preOrderId", preOrderId);

        when(preOrderRepository.findByPreOrderId(preOrderId)).thenReturn(Optional.of(preOrder));

        // when
        PreOrderResponse response = preOrderService.updateState(preOrderId, PreOrderState.OPEN_FOR_ORDER);

        // then
        assertThat(preOrder.getState()).isEqualTo(PreOrderState.OPEN_FOR_ORDER);
        assertThat(response.getState()).isEqualTo("OPEN_FOR_ORDER");
    }

    @Test
    @DisplayName("updateState: CANCELED 요청 → PreOrder.cancel() 수행(isPublic=false)")
    void updateState_cancel_success() {
        // given
        long preOrderId = 1L;

        PreOrder preOrder = PreOrder.create(new PreOrderRequest.Create(
                UUID.randomUUID(),
                "title",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(10),
                3
        ));
        ReflectionTestUtils.setField(preOrder, "preOrderId", preOrderId);

        when(preOrderRepository.findByPreOrderId(preOrderId)).thenReturn(Optional.of(preOrder));

        // when
        PreOrderResponse response = preOrderService.updateState(preOrderId, PreOrderState.CANCELED);

        // then
        assertThat(preOrder.getState()).isEqualTo(PreOrderState.CANCELED);
        assertThat(preOrder.isPublic()).isFalse();
        assertThat(response.getState()).isEqualTo("CANCELED");
        assertThat(response.isPublic()).isFalse();
    }


    @Test
    @DisplayName("reservation: execute(락) 호출 + validateQuantity가 락 내부에서 실행되고, 그 뒤에 preOrder가 호출된다")
    void reservation_calls_execute_and_orders_validate_then_preOrder() {
        // given
        long preOrderId = 10L;
        long userId = 777L;

        // PreOrderRedisDto.validateReservationDate()가 통과하도록
        // start < now < end 인 시간 범위를 만들어준다.
        PreOrderRedisDto cached = new PreOrderRedisDto(
                preOrderId,
                "product-uuid-string",
                100,
                LocalDateTime.now().minusMinutes(10),
                LocalDateTime.now().plusMinutes(10)
        );

        when(preOrderCacheService.getPreOrderCache(preOrderId)).thenReturn(cached);

        /**
         * 핵심 포인트:
         * unit test에서는 "진짜 분산락"을 잡을 수 없고 잡을 필요도 없다.
         *
         * 대신, DistributedLockComponent.execute(...)가 "락을 잡았다고 가정"하고
         * 내부 Runnable(logic)을 실행하도록 스텁한다.
         *
         * 이렇게 하면 validateQuantity(...)가 'execute 내부에서 호출'되는 상황을
         * 테스트에서 재현할 수 있다.
         */
        doAnswer(invocation -> {
            Runnable logic = invocation.getArgument(3, Runnable.class);
            logic.run(); // 락 내부에서 실행되는 로직을 직접 실행
            return null;
        }).when(distributedLockComponent).execute(anyString(), anyLong(), anyLong(), any(Runnable.class));

        // when
        PreOrderRedisDto result = preOrderLockService.reservation(preOrderId, userId);

        // then
        assertThat(result).isSameAs(cached);

        // 1) execute가 정확한 lockName/timeout으로 호출되는지 검증
        verify(distributedLockComponent).execute(
                eq("preOrderLock_%s".formatted(preOrderId)),
                eq(3000L),
                eq(3000L),
                any(Runnable.class)
        );

        // 2) validateQuantity → preOrder 호출 순서 보장 (연동 순서 테스트)
        InOrder inOrder = inOrder(preOrderCacheService, distributedLockComponent, preOrderRedisService);

        // 캐시 조회가 먼저
        inOrder.verify(preOrderCacheService).getPreOrderCache(preOrderId);

        // 락 execute 호출
        inOrder.verify(distributedLockComponent).execute(
                eq("preOrderLock_%s".formatted(preOrderId)),
                eq(3000L),
                eq(3000L),
                any(Runnable.class)
        );

        // execute 내부에서 validateQuantity가 수행되어야 한다.
        inOrder.verify(preOrderRedisService).validateQuantity(cached, userId);

        // 그 다음에 Redis 예약 등록(preOrder)
        inOrder.verify(preOrderRedisService).preOrder(getRedisKeyOfPreOrder(preOrderId), userId);
    }

    @Test
    @DisplayName("reservation: 예약 가능 시간이 아니면 즉시 예외 → 락/Redis 로직은 호출되지 않는다")
    void reservation_invalidDate_throws_and_doesNotCall_lock_or_redis() {
        // given
        long preOrderId = 10L;
        long userId = 777L;

        // start가 now 이후면 예약 가능 시간 조건을 만족하지 못한다.
        PreOrderRedisDto cached = new PreOrderRedisDto(
                preOrderId,
                "product-uuid-string",
                100,
                LocalDateTime.now().plusMinutes(10),  // ❌ now보다 이후
                LocalDateTime.now().plusMinutes(20)
        );

        when(preOrderCacheService.getPreOrderCache(preOrderId)).thenReturn(cached);

        // when & then
        assertThatThrownBy(() -> preOrderLockService.reservation(preOrderId, userId))
                .isInstanceOf(ProductException.class);

        // 예약 시간 검증에서 끊겨야 하므로 아래는 호출되면 안 된다.
        verifyNoInteractions(distributedLockComponent);
        verify(preOrderRedisService, never()).validateQuantity(any(), anyLong());
        verify(preOrderRedisService, never()).preOrder(anyString(), anyLong());
    }
}
