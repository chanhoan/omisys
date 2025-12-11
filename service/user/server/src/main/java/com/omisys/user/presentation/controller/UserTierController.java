package com.omisys.user.presentation.controller;

import com.omisys.auth.auth_dto.jwt.JwtClaim;
import com.omisys.common.domain.domain.response.ApiResponse;
import com.omisys.user.application.dto.UserTierResponse;
import com.omisys.user.application.service.UserService;
import com.omisys.user.presentation.request.UserRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/tier")
@RequiredArgsConstructor
public class UserTierController {

    private final UserService userService;

    @PreAuthorize("hasRole('ROLE_USER')")
    @GetMapping("/me")
    public ApiResponse<UserTierResponse.Get> getUserTierByUser(
            @AuthenticationPrincipal JwtClaim claim
    ) {
        return ApiResponse.ok(userService.getUserTierByUserId(claim.getUserId()));
    }

    @PreAuthorize("hasAnyRole('ROLE_MANAGER', 'ROLE_ADMIN')")
    @GetMapping("/{userId}")
    public ApiResponse<UserTierResponse.Get> getUserTierByUserId(
            @PathVariable Long userId
    ) {
        return ApiResponse.ok(userService.getUserTierByUserId(userId));
    }

    @PreAuthorize("hasAnyRole('ROLE_NANAGER', 'ROLE_ADMIN')")
    @GetMapping
    public ApiResponse<Page<UserTierResponse.Get>> getUserTierList(Pageable pageable) {
        return ApiResponse.ok(userService.getUserTierList(pageable));
    }

    @PreAuthorize("hasAnyRole('ROLE_MANAGER', 'ROLE_ADMIN')")
    @PatchMapping("/{userId}")
    public ApiResponse<Void> updateUserTier(
            @PathVariable Long userId,
            @RequestBody @Valid UserRequest.UpdateTier request
    ) {
        userService.updateUserTier(userId, request);
        return ApiResponse.ok();
    }

}
