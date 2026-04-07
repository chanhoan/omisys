package com.omisys.payment.server.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omisys.common.domain.entity.KafkaTopicConstant;
import com.omisys.payment.server.domain.model.Payment;
import com.omisys.payment.server.domain.model.PaymentHistory;
import com.omisys.payment.server.domain.model.PaymentState;
import com.omisys.payment.server.domain.model.outbox.OutboxEvent;
import com.omisys.payment.server.domain.model.outbox.OutboxStatus;
import com.omisys.payment.server.domain.repository.OutboxEventRepository;
import com.omisys.payment.server.domain.repository.PaymentHistoryRepository;
import com.omisys.payment.server.domain.repository.PaymentRepository;
import com.omisys.payment.server.exception.PaymentErrorCode;
import com.omisys.payment.server.exception.PaymentException;
import com.omisys.payment.server.presentation.request.PaymentRequest;
import com.omisys.payment.server.presentation.response.PaymentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentHistoryRepository paymentHistoryRepository;
    @Mock private OutboxEventRepository outboxEventRepository;

    @Mock private RestTemplateBuilder restTemplateBuilder;
    @Mock private RestTemplate restTemplate;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        when(restTemplateBuilder.build()).thenReturn(restTemplate);
        paymentService = new PaymentService(
                paymentRepository,
                paymentHistoryRepository,
                outboxEventRepository,
                restTemplateBuilder,
                new ObjectMapper()
        );
        ReflectionTestUtils.setField(paymentService, "originalKey", "test-secret-key");
    }

    @Test
    @DisplayName("paymentSuccess: 상태 PAYMENT 변경 + history 저장 + OutboxEvent PENDING 저장")
    void paymentSuccess_savesOutboxEvent() {
        // given
        Payment payment = buildPayment(99L, 10L, 1L, 10000L);
        when(paymentRepository.findByPaymentKey("pay_key_123")).thenReturn(Optional.of(payment));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST),
                any(HttpEntity.class), eq(Object.class)))
                .thenReturn(ResponseEntity.ok().build());

        // when
        PaymentResponse.Get result = paymentService.paymentSuccess("pay_key_123");

        // then
        assertThat(result.getOrderId()).isEqualTo(10L);
        assertThat(payment.getState()).isEqualTo(PaymentState.PAYMENT);
        verify(paymentHistoryRepository).save(any(PaymentHistory.class));

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        OutboxEvent saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(saved.getEventType()).isEqualTo(KafkaTopicConstant.PAYMENT_COMPLETED);
        assertThat(saved.getAggregateType()).isEqualTo("Payment");
    }

    @Test
    @DisplayName("paymentSuccess: OutboxEvent payload에 success=true가 포함된다")
    void paymentSuccess_outboxPayloadContainsSuccessTrue() throws Exception {
        // given
        Payment payment = buildPayment(99L, 10L, 1L, 10000L);
        when(paymentRepository.findByPaymentKey("pay_key_123")).thenReturn(Optional.of(payment));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST),
                any(HttpEntity.class), eq(Object.class)))
                .thenReturn(ResponseEntity.ok().build());

        // when
        paymentService.paymentSuccess("pay_key_123");

        // then
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        assertThat(captor.getValue().getPayload()).contains("\"success\":true");
    }

    @Test
    @DisplayName("paymentFail: 상태 CANCEL 변경 + OutboxEvent PENDING 저장 (success=false)")
    void paymentFail_savesOutboxEvent() throws Exception {
        // given
        Payment payment = buildPayment(99L, 10L, 1L, 10000L);
        when(paymentRepository.findByPaymentKey("pay_key_123")).thenReturn(Optional.of(payment));

        // when
        paymentService.paymentFail("pay_key_123");

        // then
        assertThat(payment.getState()).isEqualTo(PaymentState.CANCEL);
        verify(paymentRepository).save(payment);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        assertThat(captor.getValue().getPayload()).contains("\"success\":false");
        assertThat(captor.getValue().getEventType()).isEqualTo(KafkaTopicConstant.PAYMENT_COMPLETED);
    }

    @Test
    @DisplayName("paymentSuccess: KafkaTemplate을 직접 호출하지 않는다 (OutboxPoller에 위임)")
    void paymentSuccess_doesNotCallKafkaDirectly() {
        // given
        Payment payment = buildPayment(99L, 10L, 1L, 10000L);
        when(paymentRepository.findByPaymentKey("pay_key_123")).thenReturn(Optional.of(payment));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST),
                any(HttpEntity.class), eq(Object.class)))
                .thenReturn(ResponseEntity.ok().build());

        // when
        paymentService.paymentSuccess("pay_key_123");

        // then — KafkaTemplate 주입 없이 동작 확인
        verify(outboxEventRepository).save(any(OutboxEvent.class));
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

    private Payment buildPayment(Long paymentId, Long orderId, Long userId, Long amount) {
        Payment p = new Payment();
        p.setPaymentId(paymentId);
        p.setOrderId(orderId);
        p.setUserId(userId);
        p.setAmount(amount);
        p.setPaymentKey("pay_key_123");
        p.setState(PaymentState.PENDING);
        return p;
    }
}
