package com.omisys.common.domain.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

/**
 * 모든 서비스의 OpenAPI 문서에 인증 방식을 채워 넣는다.
 *
 * <p>서비스마다 같은 설정을 복사해 두면 한 곳만 고쳐져 어긋난다. 자동 설정으로 올려
 * common:domain 에 의존하는 서비스가 그대로 물려받게 한다.
 *
 * <p>권한 표기는 spring-security 가 있어야 하므로 {@link OmisysRoleDocAutoConfiguration} 으로
 * 분리했다.
 */
@AutoConfiguration
@ConditionalOnClass(OpenAPI.class)
public class OmisysOpenApiAutoConfiguration {

    private static final String SECURITY_SCHEME_NAME = "accessToken";

    @Bean
    public OpenAPI omisysOpenApi() {
        SecurityScheme accessToken = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.COOKIE)
                .name("accessToken")
                .description("""
                        로그인(POST /api/auth/sign-in)이 내려주는 JWT.

                        게이트웨이가 쿠키의 accessToken 을 검증한 뒤 X-User-Claims 헤더로 바꿔
                        각 서비스에 넘긴다. 브라우저는 쿠키만 실어 보내면 되고 헤더를 직접
                        만들 필요가 없다. 서비스 간 호출은 Authorization: Bearer 도 받는다.""");

        return new OpenAPI()
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME, accessToken))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }
}
