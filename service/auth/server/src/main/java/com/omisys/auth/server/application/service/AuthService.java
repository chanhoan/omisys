package com.omisys.auth.server.application.service;

import com.omisys.auth.server.application.dto.AuthResponse;
import com.omisys.auth.server.auth_dto.jwt.JwtClaim;
import com.omisys.auth.server.exception.AuthErrorCode;
import com.omisys.auth.server.exception.AuthException;
import com.omisys.auth.server.infrastructure.properties.JwtProperties;
import com.omisys.auth.server.presentation.request.AuthRequest;
import com.omisys.user_dto.infrastructure.UserDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.crypto.Data;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

import static com.omisys.auth.server.domain.JwtConstant.*;

@Slf4j
@Service
public class AuthService {

    private final UserService userService;
    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserService userService, JwtProperties jwtProperties, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.jwtProperties = jwtProperties;
        this.secretKey = createSecretKey();
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse.SignIn signIn(AuthRequest.SignIn request) {
        UserDto userData = userService.getUserByUsername(request.getUsername());

        if (userData == null
        || !passwordEncoder.matches(request.getPassword(), userData.getPassword())) {
            throw new AuthException(AuthErrorCode.SIGN_IN_FAIL);
        }

        return AuthResponse.SignIn.of(
                this.createToken(
                        JwtClaim.create(userData.getUserId(), userData.getUserName(), userData.getRole())));
    }

    private String createToken(JwtClaim jwtClaim) {

        Map<String, Object> tokenClaims = this.createClaims(jwtClaim);
        Date now = new Date(System.currentTimeMillis());
        long accessTokenExpireIn = jwtProperties.getAccessTokenExpiresIn();

        return Jwts.builder()
                .claims(tokenClaims)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTokenExpireIn * MILLI_SECOND))
                .signWith(secretKey)
                .compact();
    }

    private Map<String, Object> createClaims(JwtClaim jwtClaim) {
        return Map.of(
                USER_ID, jwtClaim.getUserId(),
                USER_NAME, jwtClaim.getUsername(),
                USER_ROLE, jwtClaim.getRole());
    }

    private SecretKey createSecretKey() {
        return new SecretKeySpec(
                jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8),
                Jwts.SIG.HS256.key().build().getAlgorithm());
    }
}
