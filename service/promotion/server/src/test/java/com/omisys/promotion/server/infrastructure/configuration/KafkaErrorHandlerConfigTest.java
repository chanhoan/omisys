package com.omisys.promotion.server.infrastructure.configuration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.backoff.BackOff;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * KafkaErrorHandlerConfig 단위 테스트 (Promotion 서비스).
 * DefaultErrorHandler의 BackOff 설정과 KafkaListenerContainerFactory 구성을 검증한다.
 *
 * 내부 구조 (Spring Kafka 3.2.4 기준):
 *   DefaultErrorHandler → failureTracker (FailedRecordTracker) → backOff
 *   AbstractKafkaListenerContainerFactory → commonErrorHandler
 * Spring Kafka 버전 업그레이드 시 extractBackOff() 필드명 확인 필요.
 */
@ExtendWith(MockitoExtension.class)
class KafkaErrorHandlerConfigTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private ConsumerFactory<String, Object> consumerFactory;

    private KafkaErrorHandlerConfig config;
    private DefaultErrorHandler handler;

    @BeforeEach
    void setUp() {
        config = new KafkaErrorHandlerConfig(kafkaTemplate);
        handler = config.defaultErrorHandler();
    }

    @Test
    @DisplayName("defaultErrorHandler: DefaultErrorHandler 인스턴스가 생성된다")
    void defaultErrorHandler_returnsDefaultErrorHandler() {
        assertThat(handler).isInstanceOf(DefaultErrorHandler.class);
    }

    @Test
    @DisplayName("defaultErrorHandler: BackOff이 ExponentialBackOffWithMaxRetries로 설정된다")
    void defaultErrorHandler_backOffIsExponentialWithMaxRetries() {
        assertThat(extractBackOff(handler)).isInstanceOf(ExponentialBackOffWithMaxRetries.class);
    }

    @Test
    @DisplayName("defaultErrorHandler: 최대 재시도 횟수는 3회로 설정된다")
    void defaultErrorHandler_maxRetriesIsThree() {
        assertThat(asExponentialBackOff(handler).getMaxRetries()).isEqualTo(3);
    }

    @Test
    @DisplayName("defaultErrorHandler: 초기 재시도 간격은 1초(1000ms)로 설정된다")
    void defaultErrorHandler_initialIntervalIs1000ms() {
        assertThat(asExponentialBackOff(handler).getInitialInterval()).isEqualTo(1_000L);
    }

    @Test
    @DisplayName("defaultErrorHandler: 재시도 간격 배율은 2.0으로 설정된다")
    void defaultErrorHandler_multiplierIs2() {
        assertThat(asExponentialBackOff(handler).getMultiplier()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("defaultErrorHandler: 최대 재시도 간격은 10초(10000ms)로 설정된다")
    void defaultErrorHandler_maxIntervalIs10000ms() {
        assertThat(asExponentialBackOff(handler).getMaxInterval()).isEqualTo(10_000L);
    }

    @Test
    @DisplayName("kafkaListenerContainerFactory: CommonErrorHandler로 DefaultErrorHandler가 등록된다")
    void kafkaListenerContainerFactory_setsCommonErrorHandler() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                config.kafkaListenerContainerFactory(consumerFactory, handler);

        assertThat(factory).isNotNull();
        // Spring Kafka 3.2.4: AbstractKafkaListenerContainerFactory.commonErrorHandler
        Object registeredHandler = ReflectionTestUtils.getField(factory, "commonErrorHandler");
        assertThat(registeredHandler).isSameAs(handler);
    }

    // Spring Kafka 3.2.4: DefaultErrorHandler → FailedRecordTracker → backOff
    private BackOff extractBackOff(DefaultErrorHandler errorHandler) {
        Object tracker = ReflectionTestUtils.getField(errorHandler, "failureTracker");
        return (BackOff) ReflectionTestUtils.getField(tracker, "backOff");
    }

    private ExponentialBackOffWithMaxRetries asExponentialBackOff(DefaultErrorHandler errorHandler) {
        return (ExponentialBackOffWithMaxRetries) extractBackOff(errorHandler);
    }
}
