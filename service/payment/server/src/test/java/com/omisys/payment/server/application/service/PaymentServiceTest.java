package com.omisys.payment.server.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omisys.common.domain.entity.KafkaTopicConstant;
import com.omisys.payment.server.domain.model.Payment;
import com.omisys.payment.server.domain.model.PaymentHistory;
import com.omisys.payment.server.domain.model.PaymentState;
import com.omisys.payment.server.domain.repository.PaymentHistoryRepository;
import com.omisys.payment.server.domain.repository.PaymentRepository;
import com.omisys.payment.server.exception.PaymentErrorCode;
import com.omisys.payment.server.exception.PaymentException;
import com.omisys.payment.server.presentation.request.PaymentRequest;
import com.omisys.payment.server.presentation.response.PaymentResponse;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentHistoryRepository paymentHistoryRepository;

    @Mock private RestTemplateBuilder restTemplateBuilder;
    @Mock private RestTemplate restTemplate;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        when(restTemplateBuilder.build()).thenReturn(restTemplate);

        paymentService = new PaymentService(
                kafkaTemplate,
                paymentRepository,
                paymentHistoryRepository,
                restTemplateBuilder
        );

        ReflectionTestUtils.setField(paymentService, "originalKey", "test-secret-key");
    }

    @Test
    @DisplayName("paymentSuccess: Toss confirm 호출 + Kafka(success=true) + 상태 PAYMENT + history 저장")
    void paymentSuccess_success_flow() throws Exception {
        // given
        PaymentRequest.Create create = new PaymentRequest.Create();
        create.setUserId(1L);
        create.setOrderId(10L);
        create.setOrderName("ORDER-NAME");
        create.setEmail("test@omisys.com");
        create.setAmount(10000L);

        Payment payment = Payment.create(create);
        payment.setPaymentId(99L);
        payment.setPaymentKey("pay_key_123");
        payment.setState(PaymentState.PENDING);

        when(paymentRepository.findByPaymentKey("pay_key_123")).thenReturn(Optional.of(payment));

        when(restTemplate.exchange(
                eq("https://api.tosspayments.com/v1/payments/confirm"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(PaymentRequest.Confirm.class)
        )).thenReturn(ResponseEntity.ok().build());

        // when
        PaymentResponse.Get result = paymentService.paymentSuccess("pay_key_123");

        // then
        assertThat(result.getOrderId()).isEqualTo(10L);
        assertThat(result.getAmount()).isEqualTo(10000L);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(KafkaTopicConstant.PAYMENT_COMPLETED), payloadCaptor.capture());

        JsonNode node = new ObjectMapper().readTree(payloadCaptor.getValue());
        assertThat(node.get("success").asBoolean()).isTrue();
        assertThat(node.get("orderId").asLong()).isEqualTo(10L);
        assertThat(node.get("paymentId").asLong()).isEqualTo(99L);

        assertThat(payment.getState()).isEqualTo(PaymentState.PAYMENT);
        verify(paymentHistoryRepository).save(any(PaymentHistory.class));
    }

    @Test
    @DisplayName("paymentFail: Kafka(success=false) + 상태 CANCEL + payment 저장")
    void paymentFail_flow() throws Exception {
        // given
        PaymentRequest.Create create = new PaymentRequest.Create();
        create.setUserId(1L);
        create.setOrderId(10L);
        create.setOrderName("ORDER-NAME");
        create.setEmail("test@omisys.com");
        create.setAmount(10000L);

        Payment payment = Payment.create(create);
        payment.setPaymentId(99L);
        payment.setPaymentKey("pay_key_123");
        payment.setState(PaymentState.PENDING);

        when(paymentRepository.findByPaymentKey("pay_key_123")).thenReturn(Optional.of(payment));

        // when
        paymentService.paymentFail("pay_key_123");

        // then
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(KafkaTopicConstant.PAYMENT_COMPLETED), payloadCaptor.capture());

        JsonNode node = new ObjectMapper().readTree(payloadCaptor.getValue());
        assertThat(node.get("success").asBoolean()).isFalse();

        assertThat(payment.getState()).isEqualTo(PaymentState.CANCEL);
        verify(paymentRepository).save(payment);
    }

    @Test
    @DisplayName("paymentSuccess: paymentKey로 결제를 못 찾으면 PAYMENT_NOT_FOUND")
    void paymentSuccess_not_found_throws() {
        // given
        when(paymentRepository.findByPaymentKey("x")).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> paymentService.paymentSuccess("x"))
                .isInstanceOf(PaymentException.class)
                .satisfies(ex -> {
                    PaymentException pe = (PaymentException) ex;
                    assertThat(pe.getStatusName()).isEqualTo(PaymentErrorCode.PAYMENT_NOT_FOUND.getMessage());
                    assertThat(pe.getMessage()).isEqualTo(PaymentErrorCode.PAYMENT_NOT_FOUND.getStatus().name());
                });
    }
}
