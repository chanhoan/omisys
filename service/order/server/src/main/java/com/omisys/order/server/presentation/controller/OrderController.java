package com.omisys.order.server.presentation.controller;

import com.omisys.auth.server.auth_dto.jwt.JwtClaim;
import com.omisys.common.domain.response.ApiResponse;
import com.omisys.order.order_dto.dto.OrderCreateRequest;
import com.omisys.order.server.application.service.OrderCreateService;
import com.omisys.order.server.application.service.OrderService;
import com.omisys.order.server.presentation.response.OrderCreateResponse;
import com.omisys.order.server.presentation.response.OrderResponse;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderCreateService orderCreateService;
    private final OrderService orderService;

    @PostMapping
    public ApiResponse<OrderCreateResponse> createOrder(
            @AuthenticationPrincipal JwtClaim userClaim,
            @RequestBody @Valid OrderCreateRequest request) {
        return ApiResponse.created(orderCreateService.createOrder(userClaim.getUserId(), request));
    }

    @PatchMapping("/{orderId}/cancel")
    public ApiResponse<Long> cancelOrder(
            @AuthenticationPrincipal JwtClaim userClaim,
            @PathVariable Long orderId) {
        return ApiResponse.ok(orderService.cancelOrder(userClaim.getUserId(), orderId));
    }

    @PatchMapping("/{orderId}/")
    public ApiResponse<Long> updateOrderAddress(
            @AuthenticationPrincipal JwtClaim userClaim,
            @PathVariable Long orderId,
            @RequestParam Long addressId) {
        return ApiResponse.ok(orderService.updateOrderAddress(userClaim.getUserId(), orderId, addressId));
    }

    /**
     * @deprecated 송장 등록의 정본(source of truth)은 {@code PATCH /api/deliveries/{deliveryId}/invoice} 이다.
     * 관리자 프론트는 이 엔드포인트 대신 delivery 서비스의 송장 등록 API를 사용해야 한다.
     * 이 엔드포인트는 내부 동기화/레거시 호환 목적으로만 유지한다.
     */
    @Deprecated
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER')")
    @PatchMapping("/{orderId}/invoice-number/{invoiceNumber}")
    public ApiResponse<Long> addOrderInvoiceNumber(
            @AuthenticationPrincipal JwtClaim userClaim,
            @PathVariable(name = "orderId") Long orderId,
            @PathVariable(name = "invoiceNumber") String invoiceNumber) {
        return ApiResponse.ok(
                orderService.registerOrderInvoiceNumber(userClaim.getUserId(), orderId, invoiceNumber));
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER')")
    @PatchMapping("/{orderId}/{orderState}")
    public ApiResponse<Long> updateOrderState(
            @AuthenticationPrincipal JwtClaim userClaim,
            @PathVariable Long orderId,
            @PathVariable String orderState) {
        return ApiResponse.ok(orderService.updateOrderState(userClaim.getUserId(), orderId, orderState));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse.OrderGetResponse> getOrder(
            @AuthenticationPrincipal JwtClaim userClaim,
            @PathVariable Long orderId) {
        return ApiResponse.ok(orderService.getOrder(userClaim.getUserId(), orderId));
    }

    @GetMapping("/me")
    public ApiResponse<Page<OrderResponse.MyOrderGetResponse>> getMyOrder(
            @AuthenticationPrincipal JwtClaim userClaim,
            Pageable pageable,
            @RequestParam(required = false, name = "keyword") String keyword) {
        return ApiResponse.ok(orderService.getMyOrder(pageable, userClaim.getUserId(), keyword));
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER')")
    @GetMapping("/all")
    public ApiResponse<Page<OrderResponse.AllOrderGetResponse>> getAllOrder(
            @AuthenticationPrincipal JwtClaim userClaim,
            Pageable pageable,
            @RequestParam(required = false, name = "user_id") Long orderUserId,
            @RequestParam(required = false, name = "product_id") String productId,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ApiResponse.ok(
                orderService.getAllOrder(pageable, userClaim.getUserId(), orderUserId, productId, state, from, to));
    }

    @DeleteMapping("/{orderId}")
    public ApiResponse<Void> deleteOrder(
            @AuthenticationPrincipal JwtClaim userClaim,
            @PathVariable Long orderId) {
        orderService.deleteOrder(userClaim.getUserId(), orderId);
        return ApiResponse.ok();
    }
}
