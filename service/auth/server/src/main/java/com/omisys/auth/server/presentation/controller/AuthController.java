package com.omisys.auth.server.presentation.controller;

import com.omisys.auth.server.application.dto.AuthResponse;
import com.omisys.auth.server.application.service.AuthService;
import com.omisys.auth.server.presentation.request.AuthRequest;
import com.omisys.common.domain.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/sign-in")
    public ApiResponse<AuthResponse.SignIn> signIn(@RequestBody AuthRequest.SignIn request) {
        return ApiResponse.ok(authService.signIn(request));
    }
}
