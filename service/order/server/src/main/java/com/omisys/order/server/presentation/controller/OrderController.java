package com.omisys.order.server.presentation.controller;

import com.omisys.auth.server.auth_dto.jwt.JwtClaim;
import com.omisys.common.domain.response.ApiResponse;
import com.omisys.order.order_dto.dto.OrderCreateRequest;
import com.omisys.order.server.application.service.OrderCreateService;
import com.omisys.order.server.application.service.OrderService;
import com.omisys.order.server.presentation.response.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    public ApiResponse<Long> createOrder(
            @AuthenticationPrincipal JwtClaim userClaim,
            @RequestBody OrderCreateRequest request) {
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
            @RequestParam(required = false, name = "product_id") String productId) {
        return ApiResponse.ok(
                orderService.getAllOrder(pageable, userClaim.getUserId(), orderUserId, productId));
    }

    @DeleteMapping("/{orderId}")
    public ApiResponse<Void> deleteOrder(
            @AuthenticationPrincipal JwtClaim userClaim,
            @PathVariable Long orderId) {
        orderService.deleteOrder(userClaim.getUserId(), orderId);
        return ApiResponse.ok();
    }
}
