package com.omisys.payment.server.application.service;

import com.omisys.payment.server.domain.model.Payment;
import com.omisys.payment.server.domain.model.PaymentHistory;
import com.omisys.payment.server.domain.model.PaymentState;
import com.omisys.payment.server.domain.repository.PaymentHistoryRepository;
import com.omisys.payment.server.domain.repository.PaymentRepository;
import com.omisys.payment.server.exception.PaymentErrorCode;
import com.omisys.payment.server.exception.PaymentException;
import com.omisys.payment.server.infrastructure.client.MessageClient;
import com.omisys.payment.server.presentation.request.PaymentRequest;
import com.omisys.payment.server.presentation.response.PaymentResponse;
import com.omisys.slack.slack_dto.dto.MessageInternalDto;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentInternalServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentHistoryRepository paymentHistoryRepository;
    @Mock private MessageClient messageClient;

    @Mock private RestTemplateBuilder restTemplateBuilder;
    @Mock private RestTemplate restTemplate;

    // ✅ @InjectMocks 쓰지 않는다 (생성자에서 build()를 즉시 호출하기 때문)
    private PaymentInternalService paymentInternalService;

    @BeforeEach
    void setUp() {
        // ✅ 서비스 생성 전에 build() stub을 반드시 걸어야 한다.
        when(restTemplateBuilder.build()).thenReturn(restTemplate);

        paymentInternalService = new PaymentInternalService(
                paymentRepository,
                paymentHistoryRepository,
                restTemplateBuilder,
                messageClient
        );

        // ✅ @Value 필드는 스프링 컨텍스트 없이 직접 주입
        ReflectionTestUtils.setField(paymentInternalService, "originalKey", "test-secret-key");
        ReflectionTestUtils.setField(paymentInternalService, "SUCCESS_URL", "http://success");
        ReflectionTestUtils.setField(paymentInternalService, "FAIL_URL", "http://fail");
    }

    @Test
    @DisplayName("createPayment: Toss 결제생성 호출 → Payment 저장 + checkoutUrl 메시지 전송")
    void createPayment_success_creates_payment_and_sends_message() {
        // given
        PaymentRequest.Create request = new PaymentRequest.Create();
        request.setUserId(1L);
        request.setOrderId(10L);
        request.setOrderName("ORDER-NAME");
        request.setEmail("test@omisys.com");
        request.setAmount(10000L);

        // Toss 응답 mock
        PaymentResponse.Create responseBody = PaymentResponse.Create.builder()
                .paymentKey("pay_key_123")
                .checkout("{url=https://checkout.toss/pay/abc}")
                .build();

        when(restTemplate.exchange(
                eq("https://api.tosspayments.com/v1/payments"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(PaymentResponse.Create.class)
        )).thenReturn(ResponseEntity.ok(responseBody));

        // when
        paymentInternalService.createPayment(request);

        // then
        // 1) 메시지 전송 검증
        ArgumentCaptor<MessageInternalDto.Create> msgCaptor = ArgumentCaptor.forClass(MessageInternalDto.Create.class);
        verify(messageClient).sendMessage(msgCaptor.capture());

        MessageInternalDto.Create sent = msgCaptor.getValue();
        assertThat(sent.getReceiverEmail()).isEqualTo("test@omisys.com");
        assertThat(sent.getMessage()).isEqualTo("https://checkout.toss/pay/abc");

        // 2) Payment 저장 검증 (paymentKey 세팅 포함)
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());

        Payment saved = paymentCaptor.getValue();
        assertThat(saved.getOrderId()).isEqualTo(10L);
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getOrderName()).isEqualTo("ORDER-NAME");
        assertThat(saved.getAmount()).isEqualTo(10000L);
        assertThat(saved.getPaymentKey()).isEqualTo("pay_key_123");
        assertThat(saved.getState()).isEqualTo(PaymentState.PENDING);
    }

    @Test
    @DisplayName("cancelPayment: Toss 결제취소 호출 → PaymentHistory 저장 + Payment 상태 CANCEL 저장")
    void cancelPayment_success_saves_history_and_updates_state() {
        // given
        PaymentRequest.Cancel request = new PaymentRequest.Cancel();
        request.setOrderId(10L);
        request.setCancelReason("user cancel");

        PaymentRequest.Create create = new PaymentRequest.Create();
        create.setUserId(1L);
        create.setOrderId(10L);
        create.setOrderName("ORDER-NAME");
        create.setEmail("test@omisys.com");
        create.setAmount(10000L);

        Payment payment = Payment.create(create);
        payment.setPaymentKey("pay_key_123");

        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(payment));

        when(restTemplate.exchange(
                eq("https://api.tosspayments.com/v1/payments/pay_key_123/cancel"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Object.class)
        )).thenReturn(ResponseEntity.ok().build());

        // when
        paymentInternalService.cancelPayment(request);

        // then
        ArgumentCaptor<PaymentHistory> historyCaptor = ArgumentCaptor.forClass(PaymentHistory.class);
        verify(paymentHistoryRepository).save(historyCaptor.capture());

        PaymentHistory history = historyCaptor.getValue();
        assertThat(history.getType()).isEqualTo(PaymentState.CANCEL);
        assertThat(history.getCancelReason()).isEqualTo("user cancel");

        assertThat(payment.getState()).isEqualTo(PaymentState.CANCEL);
        verify(paymentRepository).save(payment);
    }

    @Test
    @DisplayName("cancelPayment: orderId로 Payment를 못 찾으면 PAYMENT_NOT_FOUND")
    void cancelPayment_not_found_throws() {
        // given
        PaymentRequest.Cancel request = new PaymentRequest.Cancel();
        request.setOrderId(10L);
        request.setCancelReason("x");

        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> paymentInternalService.cancelPayment(request))
                .isInstanceOf(PaymentException.class)
                .satisfies(ex -> {
                    PaymentException pe = (PaymentException) ex;
                    assertThat(pe.getStatusName()).isEqualTo(PaymentErrorCode.PAYMENT_NOT_FOUND.getStatus().name());
                    assertThat(pe.getMessage()).isEqualTo(PaymentErrorCode.PAYMENT_NOT_FOUND.getMessage());
                });
    }
}
