package com.omisys.gateway.server.application;

import com.omisys.auth.server.auth_dto.jwt.JwtClaim;

public interface AuthService {

    JwtClaim verifyToken(String token);

}
