package com.omisys.product.infrastructure.configuration;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.TransportUtils;
import co.elastic.clients.transport.rest_client.RestClientOptions;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.net.ssl.SSLContext;
import java.nio.file.Path;

@Configuration
public class ElasticsearchClientConfig {

    @Value("${spring.elasticsearch.rest.host}")
    String host;

    @Value("${spring.elasticsearch.rest.port}")
    int port;

    @Value("${spring.elasticsearch.fingerprint}")
    String fingerprint;

    @Value("${spring.elasticsearch.account}")
    String account;

    @Value("${spring.elasticsearch.password}")
    String password;

    @Bean
    RestClientTransport restClientTransport(
            RestClient restClient, ObjectProvider<RestClientOptions> restClientOptions) {
        return new RestClientTransport(
                restClient, new JacksonJsonpMapper(), restClientOptions.getIfAvailable());
    }

    /**
     * fingerprint 가 비어 있으면 평문 HTTP 로 붙는다.
     *
     * <p>의존성 스택의 Elasticsearch 는 SSH 터널 뒤에만 열려 있어 HTTP 로 운영한다.
     * 터널이 이미 전송을 암호화하므로 자체서명 CA 를 클라이언트마다 신뢰시킬 이유가 없다.
     * HTTPS 로 운영하는 환경에서는 fingerprint 를 넣으면 종전대로 TLS 로 붙는다.
     * 비밀번호 인증은 두 경우 모두 적용된다.
     */
    @Bean
    public ElasticsearchClient elasticsearchClientWithSSL() {
        BasicCredentialsProvider credsProv = new BasicCredentialsProvider();
        credsProv.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(account, password));

        boolean useTls = fingerprint != null && !fingerprint.isBlank();
        String scheme = useTls ? "https" : "http";

        RestClient restClient =
                RestClient.builder(new HttpHost(host, port, scheme))
                        .setHttpClientConfigCallback(
                                hc -> {
                                    hc.setDefaultCredentialsProvider(credsProv);
                                    if (useTls) {
                                        SSLContext sslContext =
                                                TransportUtils.sslContextFromCaFingerprint(fingerprint);
                                        hc.setSSLContext(sslContext);
                                    }
                                    return hc;
                                })
                        .build();

        ElasticsearchTransport transport =
                new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }
}
