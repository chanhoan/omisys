package com.omisys.gateway.server.infrastructure.filter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PublicPathPolicyTest {

    @Test
    void productDetailPath_is_public() {
        var policy = new PublicPathPolicy(false);

        assertThat(policy.isPublic("/api/products/detail/00000000-0000-0000-0000-000000000001"))
                .isTrue();
    }
}
