package com.omisys.auth.server.application.service;

import com.omisys.auth.server.auth_dto.jwt.JwtClaim;
import com.omisys.auth.server.exception.AuthErrorCode;
import com.omisys.auth.server.exception.AuthException;
import com.omisys.auth.server.infrastructure.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static com.omisys.auth.server.domain.JwtConstant.*;

@Slf4j
@Service
public class AuthInternalService {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public AuthInternalService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = createSecretKey();
    }

    public JwtClaim verifyToken(String token) {
        try {
            Claims claims =
                    Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();

            return this.convert(claims);
        } catch (ExpiredJwtException e) {
            throw new AuthException(AuthErrorCode.TOEKN_EXPIRED);
        } catch (JwtException e) {
            throw new AuthException(AuthErrorCode.INVALID_TOKEN);
        }
    }

    private JwtClaim convert(Claims claims) {
        return JwtClaim.create(
                claims.get(USER_ID, Long.class),
                claims.get(USER_NAME, String.class),
                claims.get(USER_ROLE, String.class));
    }

    private SecretKey createSecretKey() {
        return new SecretKeySpec(
                jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8),
                Jwts.SIG.HS256.key().build().getAlgorithm());
    }

}
