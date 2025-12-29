package com.omisys.product.infrastructure.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omisys.auth.server.auth_dto.jwt.JwtClaim;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static com.omisys.common.domain.jwt.JwtGlobalConstant.X_USER_CLAIMS;
import static org.assertj.core.api.Assertions.assertThat;

class SecurityContextFilterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SecurityContextFilter filter = new SecurityContextFilter(objectMapper);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void X_USER_CLAIMS_없으면_인증세팅없이_그대로_체인통과() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/products");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void X_USER_CLAIMS_있으면_Authentication_세팅되고_체인통과() throws Exception {
        JwtClaim claim = new JwtClaim(1L, "chanhoan", "ROLE_USER");
        String json = objectMapper.writeValueAsString(claim);
        String encoded = URLEncoder.encode(json, StandardCharsets.UTF_8);

        var request = new MockHttpServletRequest("GET", "/api/products");
        request.addHeader(X_USER_CLAIMS, encoded);

        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(1L);
        assertThat(auth.getCredentials()).isEqualTo("chanhoan");
        assertThat(auth.getAuthorities()).extracting(Object::toString).contains("ROLE_USER");
        assertThat(auth.isAuthenticated()).isTrue();
    }
}
