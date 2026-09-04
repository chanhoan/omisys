package com.omisys.gateway.server.infrastructure.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 인증 없이 통과시킬 경로를 한 곳에서 정한다.
 *
 * <p>필터마다 같은 목록을 따로 들고 있으면 한쪽만 고쳐도 컴파일이 통과해, 열어 준 경로가
 * 다른 필터에서 막히는 일이 생긴다. 실제로 그렇게 어긋나 있었다.
 */
@Component
public class PublicPathPolicy {

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/",
            "/oauth2/",
            "/login/oauth2/",
            "/api/users/sign-up",
            "/payments/success",
            "/payments/fail",
            "/api/search",
            "/api/products/search",
            "/api/products/detail",
            "/api/preorder/search",
            "/api/categories/search"
    );

    /** OpenAPI 문서와 Swagger UI. 열어 둘지는 프로파일이 정한다. */
    private static final List<String> API_DOCS_PATHS = List.of(
            "/api-docs",
            "/swagger-ui",
            "/webjars/swagger-ui",
            "/v3/api-docs"
    );

    /**
     * 운영에서는 닫아 둔다. 스펙을 열면 관리자 전용을 포함한 모든 엔드포인트와 요청·응답
     * 스키마가 인증 없이 드러난다. 로컬에서만 켠다.
     */
    private final boolean apiDocsPublic;

    public PublicPathPolicy(@Value("${gateway.api-docs.public:false}") boolean apiDocsPublic) {
        this.apiDocsPublic = apiDocsPublic;
    }

    public boolean isPublic(String path) {
        if (PUBLIC_PATHS.stream().anyMatch(path::startsWith)) {
            return true;
        }
        return apiDocsPublic && API_DOCS_PATHS.stream().anyMatch(path::startsWith);
    }
}
