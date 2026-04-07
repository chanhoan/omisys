package com.omisys.promotion.server.infrastructure.configuration;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

@Configuration
@RequiredArgsConstructor
public class KafkaErrorHandlerConfig {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 쿠폰 발급 소비 실패 시 ExponentialBackOff로 3회 재시도 후 DLT로 라우팅.
     * DLT 토픽명: 원본 토픽 + ".DLT" (파티션 유지)
     * 재시도 간격: 1s → 2s → 4s (최대 10s)
     */
    @Bean
    public DefaultErrorHandler defaultErrorHandler() {
        var recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + ".DLT", record.partition()));

        var backOff = new ExponentialBackOffWithMaxRetries(3);
        backOff.setInitialInterval(1_000L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(10_000L);

        return new DefaultErrorHandler(recoverer, backOff);
    }

    /**
     * 기본 KafkaListenerContainerFactory에 DefaultErrorHandler 주입.
     * Spring Boot 자동 구성 factory를 오버라이드한다.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            DefaultErrorHandler defaultErrorHandler) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, Object>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(defaultErrorHandler);
        return factory;
    }
}
