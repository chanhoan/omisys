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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class PaymentInternalService {

    @Value("${TOSS_SECRET_KEY}")
    private String originalKey;

    @Value("${PAYMENT.SUCCESS_URL}")
    private String SUCCESS_URL;

    @Value("${PAYMENT.FAIL_URL}")
    private String FAIL_URL;

    private final String tossPaymentUrl = "https://api.tosspayments.com/v1/payments";

    private final PaymentRepository paymentRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final RestTemplate restTemplate;
    private final MessageClient messageClient;

    public PaymentInternalService(
            PaymentRepository paymentRepository,
            PaymentHistoryRepository paymentHistoryRepository,
            RestTemplateBuilder restTemplateBuilder,
            MessageClient messageClient) {
        this.paymentRepository = paymentRepository;
        this.paymentHistoryRepository = paymentHistoryRepository;
        this.restTemplate = restTemplateBuilder.build();
        this.messageClient = messageClient;
    }

    @Transactional
    public void createPayment(PaymentRequest.Create request) {

        String secretKey = Base64.getEncoder().encodeToString(originalKey.getBytes());
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(secretKey);

            PaymentRequest.CreateExt body = new PaymentRequest.CreateExt();
            body.setOrderId(request.getOrderId());
            body.setOrderName(request.getOrderName());
            body.setAmount(request.getAmount());
            body.setSuccessUrl(SUCCESS_URL);
            body.setFailUrl(FAIL_URL);

            HttpEntity<PaymentRequest.CreateExt> entity = new HttpEntity<>(body, headers);

            ResponseEntity<PaymentResponse.Create> response = restTemplate.exchange(
                    tossPaymentUrl, HttpMethod.POST, entity, PaymentResponse.Create.class
            );

            Payment payment = Payment.create(request);
            payment.setPaymentKey(Objects.requireNonNull(response.getBody()).getPaymentKey());

            sendMessage(response.getBody().getCheckout(), request.getEmail());

            paymentRepository.save(payment);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new PaymentException(PaymentErrorCode.INVALID_PARAMETER);
        }

    }

    @Transactional
    public void cancelPayment(PaymentRequest.Cancel request) {
        Payment payment = paymentRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        try {
            String secretKey = Base64.getEncoder().encodeToString(originalKey.getBytes());

            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(secretKey);

            PaymentRequest.Cancel body = new PaymentRequest.Cancel();
            body.setCancelReason(request.getCancelReason());

            HttpEntity<PaymentRequest.Cancel> entity = new HttpEntity<>(body, headers);

            restTemplate.exchange(
                    tossPaymentUrl + "/" + payment.getPaymentKey() + "/cancel",
                    HttpMethod.POST,
                    entity,
                    Object.class
            );

            PaymentHistory history = PaymentHistory.cancel(payment, request.getCancelReason());
            paymentHistoryRepository.save(history);

            payment.setState(PaymentState.CANCEL);
            paymentRepository.save(payment);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new PaymentException(PaymentErrorCode.INVALID_PARAMETER);
        }
    }

    public PaymentResponse.Get getPaymentAndHistoryByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        List<PaymentHistory> histories = paymentHistoryRepository.findByPayment_PaymentId(payment.getPaymentId());

        return PaymentResponse.Get.builder()
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrderId())
                .orderName(payment.getOrderName())
                .state(payment.getState())
                .amount(payment.getAmount())
                .createdAt(payment.getCreatedAt())
                .histories(histories.stream()
                        .map(this::convertToPaymentDto)
                        .toList())
                .build();
    }

    private PaymentResponse.PaymentHistoryDto convertToPaymentDto(PaymentHistory history) {

        return PaymentResponse.PaymentHistoryDto.builder()
                .amount(history.getAmount())
                .type(history.getType())
                .cancelReason(history.getCancelReason())
                .createdAt(history.getCreatedAt())
                .build();
    }

    private void sendMessage(Object checkout, String email) {
        try {
            String checkoutUrl = checkout.toString().replace("{url=","").replace("}","");

            MessageInternalDto.Create messageRequest = new MessageInternalDto.Create();
            messageRequest.setReceiverEmail(email);
            messageRequest.setMessage(checkoutUrl);

            messageClient.sendMessage(messageRequest);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new PaymentException(PaymentErrorCode.INVALID_PARAMETER);
        }
    }
}
