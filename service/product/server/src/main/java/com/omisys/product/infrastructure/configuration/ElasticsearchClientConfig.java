package com.omisys.product.infrastructure.configuration;

import co.elastic.clients.transport.TransportUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import javax.net.ssl.SSLContext;

@Configuration
// 리포지토리 인터페이스가 있는 패키지 경로를 지정하세요
@EnableElasticsearchRepositories(basePackages = "com.omisys.product.domain.repository")
public class ElasticsearchClientConfig extends ElasticsearchConfiguration {

    @Value("${spring.elasticsearch.rest.host}")
    private String host;

    @Value("${spring.elasticsearch.rest.port}")
    private int port;

    @Value("${spring.elasticsearch.fingerprint}")
    private String fingerprint;

    @Value("${spring.elasticsearch.account}")
    private String account;

    @Value("${spring.elasticsearch.password}")
    private String password;

    @Override
    public ClientConfiguration clientConfiguration() {
        // 1. Fingerprint를 사용하여 보안이 강화된 SSLContext 생성
        SSLContext sslContext = TransportUtils.sslContextFromCaFingerprint(fingerprint);

        // 2. ClientConfiguration을 통해 모든 설정 통합
        return ClientConfiguration.builder()
                .connectedTo(host + ":" + port)
                .usingSsl(sslContext) // SSL 및 Fingerprint 적용 (검증 유지)
                .withBasicAuth(account, password) // 인증 정보 적용
                .build();
    }
}