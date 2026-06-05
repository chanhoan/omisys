package com.omisys.delivery.server.presentation.controller;

import com.omisys.auth.server.auth_dto.jwt.JwtClaim;
import com.omisys.common.domain.response.ApiResponse;
import com.omisys.delivery.server.application.service.DeliveryService;
import com.omisys.delivery.server.presentation.response.DeliveryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    @GetMapping("/{deliveryId}")
    public ApiResponse<DeliveryResponse.Get> getDelivery(
            @AuthenticationPrincipal JwtClaim userClaim,
            @PathVariable Long deliveryId) {
        return ApiResponse.ok(deliveryService.getDelivery(userClaim.getUserId(), deliveryId));
    }

    @GetMapping("/me")
    public ApiResponse<Page<DeliveryResponse.MyGet>> getMyDelivery(
            @AuthenticationPrincipal JwtClaim userClaim,
            Pageable pageable) {
        return ApiResponse.ok(deliveryService.getMyDelivery(userClaim.getUserId(), pageable));
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER')")
    @GetMapping("/all")
    public ApiResponse<Page<DeliveryResponse.AdminGet>> getAllDelivery(
            @AuthenticationPrincipal JwtClaim userClaim,
            Pageable pageable,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String state) {
        return ApiResponse.ok(deliveryService.getAllDelivery(pageable, userId, state));
    }

    @GetMapping("/{deliveryId}/tracking")
    public ApiResponse<List<DeliveryResponse.TrackingHistory>> getTrackingHistory(
            @AuthenticationPrincipal JwtClaim userClaim,
            @PathVariable Long deliveryId) {
        return ApiResponse.ok(deliveryService.getTrackingHistory(userClaim.getUserId(), deliveryId));
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER')")
    @PatchMapping("/{deliveryId}/invoice")
    public ApiResponse<Long> registerInvoice(
            @AuthenticationPrincipal JwtClaim userClaim,
            @PathVariable Long deliveryId,
            @RequestParam String courier,
            @RequestParam String invoiceNumber) {
        return ApiResponse.ok(deliveryService.registerInvoice(deliveryId, courier, invoiceNumber));
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER')")
    @PatchMapping("/{deliveryId}/state/{state}")
    public ApiResponse<Long> updateState(
            @AuthenticationPrincipal JwtClaim userClaim,
            @PathVariable Long deliveryId,
            @PathVariable String state) {
        return ApiResponse.ok(deliveryService.updateState(deliveryId, state));
    }
}
