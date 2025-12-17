package com.omisys.product.infrastructure.configuration;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.TransportUtils;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import javax.net.ssl.SSLContext;

@Configuration
@EnableElasticsearchRepositories(basePackages = "com.omisys.product.domain.repository")
public class ElasticsearchClientConfig {

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
    public RestClient restClient() {
        // 1. Fingerprint 정제 및 SSLContext 생성
        String cleanedFingerprint = fingerprint.trim();
        if (cleanedFingerprint.contains("=")) {
            cleanedFingerprint = cleanedFingerprint.split("=")[1].trim();
        }

        SSLContext sslContext = TransportUtils.sslContextFromCaFingerprint(cleanedFingerprint);

        // 2. 물리적인 RestClient 생성 (모든 보안 필터 주입)
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(account, password));

        return RestClient.builder(new HttpHost(host, port, "https"))
                .setHttpClientConfigCallback(hc -> hc
                        .setSSLContext(sslContext)
                        .setSSLHostnameVerifier((hostname, session) -> true) // Hostname 검증 우회
                        .setDefaultCredentialsProvider(credentialsProvider))
                .build();
    }

    @Bean
    @Primary
    public ElasticsearchClient elasticsearchClient(RestClient restClient) {
        // 3. Jackson 매퍼와 함께 트랜스포트 계층 생성
        RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }

    @Bean(name = "elasticsearchOperations") // 이 이름이 리포지토리 자동 주입의 핵심입니다
    @Primary
    public ElasticsearchOperations elasticsearchOperations(ElasticsearchClient elasticsearchClient) {
        // 4. 리포지토리가 실제로 사용하는 Operations 빈을 우리가 만든 클라이언트로 생성
        return new ElasticsearchTemplate(elasticsearchClient);
    }
}