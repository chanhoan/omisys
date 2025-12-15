package com.omisys.product.infrastructure.configuration;

import com.omisys.product.infrastructure.messaging.PreOrderProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(value = "kafka.enabled", matchIfMissing = true)
public class KafkaConfig {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Bean
    public PreOrderProducer preOrderProducer() {
        return new PreOrderProducer(kafkaTemplate);
    }

}
