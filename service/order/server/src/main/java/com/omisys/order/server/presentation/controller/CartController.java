package com.omisys.order.server.presentation.controller;

import com.omisys.auth.server.auth_dto.jwt.JwtClaim;
import com.omisys.common.domain.response.ApiResponse;
import com.omisys.order.server.application.service.CartService;
import com.omisys.order.server.presentation.request.CartProductRequest;
import com.omisys.order.server.presentation.response.CartProductResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ApiResponse<List<CartProductResponse>> getCartList(
            @AuthenticationPrincipal JwtClaim userClaim) {
        return ApiResponse.ok(cartService.getCart(userClaim.getUserId()));
    }

    @PostMapping("/products")
    public ApiResponse<Void> addCart(
            @AuthenticationPrincipal JwtClaim userClaim,
            @RequestBody @Valid CartProductRequest cartProductRequest) {
        cartService.addCart(userClaim.getUserId(), cartProductRequest);
        return ApiResponse.created(null);
    }

    @PatchMapping("/products")
    public ApiResponse<Void> updateCart(
            @AuthenticationPrincipal JwtClaim userClaim,
            @RequestBody @Valid CartProductRequest cartProductRequest) {
        cartService.updateCart(userClaim.getUserId(), cartProductRequest);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/products/{productId}")
    public ApiResponse<Void> deleteCart(
            @AuthenticationPrincipal JwtClaim userClaim,
            @PathVariable String productId) {
        cartService.deleteCart(userClaim.getUserId(), productId);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/clear")
    public ApiResponse<Void> clearCart(@AuthenticationPrincipal JwtClaim userClaim) {
        cartService.clearCart(userClaim.getUserId());
        return ApiResponse.ok(null);
    }

}
