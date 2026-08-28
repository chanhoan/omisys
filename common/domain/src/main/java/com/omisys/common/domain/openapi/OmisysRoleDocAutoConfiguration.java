package com.omisys.common.domain.openapi;

import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 엔드포인트에 필요한 권한을 OpenAPI 설명에 적어 준다.
 *
 * <p>PreAuthorize 를 참조하므로 인증 스킴 설정과 분리해 둔다. search 처럼 spring-security 가
 * 없는 서비스에서는 이 클래스가 아예 로드되지 않아야 한다. 한 클래스에 묶어 두었더니 그런
 * 서비스가 기동조차 못 했다.
 */
@AutoConfiguration
@ConditionalOnClass({OperationCustomizer.class, PreAuthorize.class})
public class OmisysRoleDocAutoConfiguration {

    /** hasRole('X') · hasAnyRole('X', 'Y') 안의 롤 이름만 뽑는다. */
    private static final Pattern ROLE_PATTERN = Pattern.compile("'([A-Z_]+)'");

    /** 엔드포인트마다 손으로 달면 코드와 문서가 갈라지므로, 실제 어노테이션을 읽어 쓴다. */
    @Bean
    public OperationCustomizer omisysRequiredRoleCustomizer() {
        return (operation, handlerMethod) -> {
            PreAuthorize preAuthorize = findPreAuthorize(handlerMethod);
            if (preAuthorize == null) {
                return operation;
            }

            Set<String> roles = extractRoles(preAuthorize.value());
            if (roles.isEmpty()) {
                return operation;
            }

            String note = "필요 권한: " + String.join(" 또는 ", roles);
            String description = operation.getDescription();
            operation.setDescription(
                    description == null || description.isBlank() ? note : description + "\n\n" + note);
            return operation;
        };
    }

    /**
     * 메서드 보안이 켜져 있으면 컨트롤러가 프록시로 감싸여, 핸들러가 들고 있는 메서드에서
     * 어노테이션이 바로 보이지 않을 수 있다. 실제 구현 메서드까지 되짚어 찾는다.
     */
    private static PreAuthorize findPreAuthorize(HandlerMethod handlerMethod) {
        Method method = handlerMethod.getMethod();
        PreAuthorize found = AnnotatedElementUtils.findMergedAnnotation(method, PreAuthorize.class);
        if (found != null) {
            return found;
        }

        Method specific = AopUtils.getMostSpecificMethod(method, handlerMethod.getBeanType());
        found = AnnotatedElementUtils.findMergedAnnotation(specific, PreAuthorize.class);
        if (found != null) {
            return found;
        }

        return AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), PreAuthorize.class);
    }

    private static Set<String> extractRoles(String expression) {
        Set<String> roles = new LinkedHashSet<>();
        Matcher matcher = ROLE_PATTERN.matcher(expression);
        while (matcher.find()) {
            roles.add(matcher.group(1));
        }
        return roles;
    }
}
