package com.omisys.user.presentation.controller;

import com.omisys.auth.server.auth_dto.jwt.JwtClaim;
import com.omisys.common.domain.response.ApiResponse;
import com.omisys.user.application.service.UserDeviceService;
import com.omisys.user.presentation.request.UserDeviceRequest;
import com.omisys.user.presentation.response.UserDeviceStatusResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me/devices")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_MANAGER', 'ROLE_ADMIN')")
public class UserDeviceController {
    private final UserDeviceService userDeviceService;

    @GetMapping("/{deviceId}")
    public ApiResponse<UserDeviceStatusResponse> get(
            @AuthenticationPrincipal JwtClaim claim,
            @PathVariable @NotBlank @Size(max = 128) String deviceId) {
        return ApiResponse.ok(new UserDeviceStatusResponse(
                userDeviceService.isRegistered(claim.getUserId(), deviceId)));
    }

    @PutMapping("/{deviceId}")
    public ApiResponse<Void> register(
            @AuthenticationPrincipal JwtClaim claim,
            @PathVariable @NotBlank @Size(max = 128) String deviceId,
            @RequestBody @Valid UserDeviceRequest request) {
        userDeviceService.register(claim.getUserId(), deviceId, request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{deviceId}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal JwtClaim claim,
            @PathVariable @NotBlank @Size(max = 128) String deviceId) {
        userDeviceService.delete(claim.getUserId(), deviceId);
        return ApiResponse.ok();
    }
}
