package com.omisys.user.presentation.controller;

import com.omisys.auth.auth_dto.jwt.JwtClaim;
import com.omisys.common.domain.domain.response.ApiResponse;
import com.omisys.user.application.dto.UserResponse;
import com.omisys.user.application.service.UserService;
import com.omisys.user.presentation.request.UserRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/sign-up")
    public ApiResponse<Void> signUp(@RequestBody UserRequest.Create request) {
        userService.createUser(request);
        return ApiResponse.created(null);
    }

    @PreAuthorize("hasRole('ROLE_USER')")
    @GetMapping("/me")
    public ApiResponse<UserResponse.Info> getMyPage(
            @AuthenticationPrincipal JwtClaim jwtClaim
    ) {
        return ApiResponse.ok(userService.getUserById(jwtClaim.getUserId()));
    }

    @PreAuthorize("hasAnyRole('ROLE_MANAGER', 'ROLE_ADMIN')")
    @GetMapping("/{userId}")
    public ApiResponse<UserResponse.Info> getUser(@PathVariable Long userId) {
        return ApiResponse.ok(userService.getUserById(userId));
    }

    @PreAuthorize("hasAnyRole('ROLE_MANAGER', 'ROLE_ADMIN')")
    @GetMapping
    public ApiResponse<List<UserResponse.Info>> getUserList() {
        return ApiResponse.ok(userService.getUserList());
    }

    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_MANAGER', 'ROLE_ADMIN')")
    @PatchMapping("/reset-password")
    public ApiResponse<Void> updateUserPassword(
            @RequestBody @Valid UserRequest.UpdatePassword request,
            @AuthenticationPrincipal JwtClaim jwtClaim
    ) {
        userService.updateUserPassword(jwtClaim.getUserId(), request);
        return ApiResponse.ok();
    }

    @PreAuthorize("hasAnyRole('ROLE_MANAGER', 'ROLE_ADMIN')")
    @DeleteMapping("/{userId}")
    public ApiResponse<Void> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ApiResponse.ok();
    }

}
