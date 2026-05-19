package com.omisys.common.domain.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class InternalSecretFilterTest {

    @Test
    void nonInternalPath_passesWithoutSecret() throws Exception {
        InternalSecretFilter filter = new InternalSecretFilter("secret");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void internalPath_withoutSecret_returnsForbidden() throws Exception {
        InternalSecretFilter filter = new InternalSecretFilter("secret");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void internalPath_withMatchingSecret_passes() throws Exception {
        InternalSecretFilter filter = new InternalSecretFilter("secret");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/orders");
        request.addHeader(InternalSecretConstant.X_INTERNAL_SECRET, "secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isSameAs(request);
    }
}
