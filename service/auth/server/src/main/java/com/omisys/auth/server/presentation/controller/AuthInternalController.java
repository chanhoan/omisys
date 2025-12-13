package com.omisys.auth.server.presentation.controller;

import com.omisys.auth.server.application.service.AuthInternalService;
import com.omisys.auth.server.auth_dto.jwt.JwtClaim;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/auth")
@RequiredArgsConstructor
public class AuthInternalController {

    private final AuthInternalService authInternalService;

    @GetMapping("/verify")
    public JwtClaim verifyToken(@RequestHeader("Authorization") String token) {
        return authInternalService.verifyToken(token);
    }
}
