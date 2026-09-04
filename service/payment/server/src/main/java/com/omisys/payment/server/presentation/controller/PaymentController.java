package com.omisys.payment.server.presentation.controller;

import com.omisys.common.domain.response.ApiResponse;
import com.omisys.payment.server.application.service.PaymentService;
import com.omisys.payment.server.presentation.response.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Value("${FRONTEND_BASE_URL:http://localhost:3000}")
    private String frontendBaseUrl;

    @GetMapping("/payments/success")
    public ResponseEntity<Void> paymentSuccess(@RequestParam String paymentKey) {
        PaymentResponse.Get response = paymentService.paymentSuccess(paymentKey);
        return redirectToFrontend("success", response.getOrderId().toString(), null);
    }

    @GetMapping("/payments/fail")
    public ResponseEntity<Void> paymentFail(
            @RequestParam String code,
            @RequestParam String message,
            @RequestParam(required = false) String orderId) {
        return redirectToFrontend("fail", orderId, message);
    }

    private ResponseEntity<Void> redirectToFrontend(String status, String orderId, String reason) {
        UriComponentsBuilder redirect = UriComponentsBuilder
                .fromUriString(StringUtils.trimTrailingCharacter(frontendBaseUrl, '/'))
                .path("/checkout/result")
                .queryParam("status", status);
        if (orderId != null) {
            redirect.queryParam("orderId", orderId);
        }
        if (reason != null) {
            redirect.queryParam("reason", reason);
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(redirect.build().toUri())
                .build();
    }

    @PreAuthorize("hasAnyRole('ROLE_MANAGER', 'ROLE_ADMIN')")
    @GetMapping("/api/payments/all")
    public ApiResponse<Page<PaymentResponse.Get>> getAllPayments(Pageable pageable,
                                                                 @RequestParam(required = false) String userId,
                                                                 @RequestParam(required = false) String paymentKey,
                                                                 @RequestParam(required = false) String paymentId,
                                                                 @RequestParam(required = false) String orderId,
                                                                 @RequestParam(required = false) String state) {
        return ApiResponse.ok(paymentService.getAllPayments(pageable, userId, paymentKey, paymentId,
                orderId, state));
    }

}
