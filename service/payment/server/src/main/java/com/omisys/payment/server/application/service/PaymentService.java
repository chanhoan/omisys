package com.omisys.payment.server.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omisys.common.domain.entity.KafkaTopicConstant;
import com.omisys.payment.server.domain.model.Payment;
import com.omisys.payment.server.domain.model.PaymentHistory;
import com.omisys.payment.server.domain.model.PaymentState;
import com.omisys.payment.server.domain.repository.PaymentHistoryRepository;
import com.omisys.payment.server.domain.repository.PaymentRepository;
import com.omisys.payment.server.exception.PaymentErrorCode;
import com.omisys.payment.server.exception.PaymentException;
import com.omisys.payment.server.infrastructure.event.PaymentCompletedEvent;
import com.omisys.payment.server.presentation.request.PaymentRequest;
import com.omisys.payment.server.presentation.response.PaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

@Slf4j
@Service
public class PaymentService {

    @Value("${TOSS_SECRET_KEY}")
    private String originalKey;

    private final String tossPaymentUrl = "https://api.tosspayments.com/v1/payments";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final PaymentRepository paymentRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final RestTemplate restTemplate;

    public PaymentService(
            KafkaTemplate<String, Object> kafkaTemplate,
            PaymentRepository paymentRepository,
            PaymentHistoryRepository paymentHistoryRepository,
            RestTemplateBuilder restTemplateBuilder) {
        this.kafkaTemplate = kafkaTemplate;
        this.paymentRepository = paymentRepository;
        this.paymentHistoryRepository = paymentHistoryRepository;
        this.restTemplate = restTemplateBuilder.build();
    }

    @Transactional
    public PaymentResponse.Get paymentSuccess(String paymentKey) {
        Payment payment = paymentRepository.findByPaymentKey(paymentKey)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(Base64.getEncoder().encodeToString(originalKey.getBytes()));

        PaymentRequest.Confirm body = new PaymentRequest.Confirm();
        body.setPaymentKey(paymentKey);
        body.setOrderId(payment.getOrderId());
        body.setAmount(payment.getAmount());

        HttpEntity<PaymentRequest.Confirm> entity = new HttpEntity<>(body, headers);

        restTemplate.exchange(
                tossPaymentUrl + "/confirm",
                HttpMethod.POST,
                entity,
                PaymentRequest.Confirm.class);

        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .userId(payment.getUserId())
                .success(true)
                .build();

        kafkaTemplateSend(event);

        payment.setState(PaymentState.PAYMENT);
        PaymentHistory history = PaymentHistory.create(payment);
        paymentHistoryRepository.save(history);

        return PaymentResponse.Get.builder()
                .orderId(payment.getOrderId())
                .orderName(payment.getOrderName())
                .amount(payment.getAmount())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    @Transactional
    public PaymentResponse.Get paymentFail(String paymentKey) {
        Payment payment = paymentRepository.findByPaymentKey(paymentKey)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .userId(payment.getUserId())
                .success(false)
                .build();

        kafkaTemplateSend(event);

        payment.setState(PaymentState.CANCEL);
        paymentRepository.save(payment);

        return PaymentResponse.Get.builder()
                .orderId(payment.getOrderId())
                .orderName(payment.getOrderName())
                .amount(payment.getAmount())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    public Page<PaymentResponse.Get> getAllPayments(
            Pageable pageable,
            String userId,
            String paymentKey,
            String paymentId,
            String orderId,
            String state) {
        Page<Payment> payments = paymentRepository.findBySearchOption(
                pageable,
                userId,
                paymentKey,
                paymentId,
                orderId,
                state
        );

        return payments.map(this::convertToPaymentDto);
    }

    private void kafkaTemplateSend(PaymentCompletedEvent event) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonMessage = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(KafkaTopicConstant.PAYMENT_COMPLETED, jsonMessage);

        } catch (Exception e) {
            log.error(e.getMessage());
            throw new PaymentException(PaymentErrorCode.INVALID_PARAMETER);
        }
    }

    private PaymentResponse.Get convertToPaymentDto(Payment payment) {

        return PaymentResponse.Get.builder()
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrderId())
                .orderName(payment.getOrderName())
                .state(payment.getState())
                .amount(payment.getAmount())
                .createdAt(payment.getCreatedAt())
                .build();
    }

}
