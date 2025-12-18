package com.omisys.product.presentation.controller;

import com.omisys.auth.server.auth_dto.jwt.JwtClaim;
import com.omisys.common.domain.response.ApiResponse;
import com.omisys.product.application.preorder.PreOrderFacadeService;
import com.omisys.product.application.preorder.PreOrderService;
import com.omisys.product.domain.model.PreOrderState;
import com.omisys.product.presentation.request.PreOrderRequest;
import com.omisys.product.presentation.response.PreOrderResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/preorders")
@RequiredArgsConstructor
public class PreOrderController {

    private final PreOrderService preOrderService;
    private final PreOrderFacadeService preOrderFacadeService;

    @PostMapping("/{preOrderId}/order")
    public ApiResponse<Void> preOrder(
            @NotNull @PathVariable("preOrderId") Long preOrderId,
            @NotNull @RequestParam("addressId") Long addressId,
            @AuthenticationPrincipal JwtClaim jwtClaim) {
        preOrderFacadeService.preOrder(preOrderId, addressId, jwtClaim.getUserId());
        return ApiResponse.ok();
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER')")
    @PostMapping
    public ApiResponse<Void> createPreOrder(@RequestBody @Valid PreOrderRequest.Create request) {
        preOrderService.createPreOrder(request);
        return ApiResponse.created(null);
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER')")
    @PatchMapping
    public ApiResponse<PreOrderResponse> updatePreOrder(
            @RequestBody @Valid PreOrderRequest.Update request) {
        preOrderService.updatePreOrder(request);
        return ApiResponse.ok();
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER')")
    @DeleteMapping("/{preOrderId}")
    public ApiResponse<Void> deletePreOrder(@NotNull @PathVariable("preOrderId") Long preOrderId) {
        preOrderService.deletePreOrder(preOrderId);
        return ApiResponse.ok();
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER')")
    @PatchMapping("/{preOrderId}/open")
    public ApiResponse<PreOrderResponse> openPreOrder(
            @NotNull @PathVariable("preOrderId") Long preOrderId) {
        return ApiResponse.ok(preOrderService.updateState(preOrderId, PreOrderState.OPEN_FOR_ORDER));
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER')")
    @PatchMapping("/{preOrderId}/cancel")
    public ApiResponse<PreOrderResponse> cancelPreOrder(
            @NotNull @PathVariable("preOrderId") Long preOrderId) {
        return ApiResponse.ok(preOrderService.updateState(preOrderId, PreOrderState.CANCELED));
    }

    @GetMapping("/search/{preOrderId}")
    public ApiResponse<PreOrderResponse> getPreOrder(
            @NotNull @PathVariable("preOrderId") Long preOrderId) {
        return ApiResponse.ok(preOrderService.getPreOrder(preOrderId));
    }

    @GetMapping("/search")
    public ApiResponse<Page<PreOrderResponse>> getPreOrderList(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "30") @Min(1) int size) {
        return ApiResponse.ok(preOrderService.getPreOrderList(page, size));
    }
}
