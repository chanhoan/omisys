package com.omisys.gateway.server.infrastructure.feign;

import com.omisys.auth.server.auth_dto.jwt.JwtClaim;
import com.omisys.gateway.server.application.AuthService;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "auth", configuration = AuthService.class)
public interface AuthClient extends AuthService {

    @GetMapping("/internal/auth/verify")
    JwtClaim verifyToken(@RequestHeader("Authorization") String token);

}
