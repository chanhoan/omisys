package com.omisys.product.infrastructure.configuration;

import co.elastic.clients.transport.TransportUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
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

    @Bean
    @Primary
    @Override
    public ClientConfiguration clientConfiguration() {
        // 1. Fingerprint로 SSLContext 생성 (이미 확인하신 그 값 그대로 사용)
        SSLContext sslContext = TransportUtils.sslContextFromCaFingerprint(fingerprint.trim());

        return ClientConfiguration.builder()
                .connectedTo(host + ":" + port)
                .usingSsl(sslContext, ((hostname, session) -> true))
                // 2. 중요: 호스트네임 검증기 추가
                // Fingerprint로 신뢰는 확보했지만, CN=es01과 실제 접속 주소 간의 검증을 통과시킵니다.
                .withBasicAuth(account, password)
                .build();
    }
}