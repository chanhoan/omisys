package com.omisys.user.presentation.controller;

import com.omisys.auth.server.auth_dto.jwt.JwtClaim;
import com.omisys.common.domain.domain.response.ApiResponse;
import com.omisys.user.application.dto.AddressResponse;
import com.omisys.user.application.service.AddressService;
import com.omisys.user.presentation.request.AddressRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/address")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @PostMapping
    public ApiResponse<Void> createAddress(
            @RequestBody AddressRequest.Create request,
            @AuthenticationPrincipal JwtClaim claim
    ) {
        addressService.createAddress(claim.getUserId(), request);
        return ApiResponse.created(null);
    }

    @PreAuthorize("hasRole('ROLE_USER')")
    @GetMapping("/me")
    public ApiResponse<List<AddressResponse.Get>> getAddressByUserId(
            @AuthenticationPrincipal JwtClaim claim
    ) {
        return ApiResponse.ok(addressService.getAddressByUserId(claim.getUserId()));
    }

    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_MANAGER')")
    @GetMapping
    public ApiResponse<List<AddressResponse.Get>> getAddressList() {
        return ApiResponse.ok(addressService.getAddressList());
    }

    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN', 'ROLE_MANAGER')")
    @PatchMapping("/{addressId}")
    public ApiResponse<Void> updateAddress(
            @PathVariable Long addressId,
            @RequestBody @Valid AddressRequest.Update request,
            @AuthenticationPrincipal JwtClaim claim
    ) {
        addressService.updateAddress(claim.getUserId(), addressId, request);
        return ApiResponse.ok();
    }

    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN', 'ROLE_MANAGER')")
    @DeleteMapping("/{addressId}")
    public ApiResponse<Void> deleteAddress(
            @PathVariable Long addressId,
            @AuthenticationPrincipal JwtClaim claim
    ) {
        addressService.deleteAddress(claim.getUserId(), addressId);
        return ApiResponse.ok();
    }

}
