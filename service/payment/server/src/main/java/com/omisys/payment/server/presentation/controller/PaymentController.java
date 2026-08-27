package com.omisys.payment.server.presentation.controller;

import com.omisys.common.domain.response.ApiResponse;
import com.omisys.payment.server.application.service.PaymentService;
import com.omisys.payment.server.presentation.response.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final TemplateEngine templateEngine;

    @GetMapping("/payments/success")
    public ResponseEntity<String> paymentSuccess(@RequestParam String paymentKey) {
        PaymentResponse.Get response = paymentService.paymentSuccess(paymentKey);
        Context context = new Context();
        context.setVariable("orderId", response.getOrderId());
        context.setVariable("orderName", response.getOrderName());
        context.setVariable("amount", response.getAmount());
        context.setVariable("createdAt", response.getCreatedAt());

        String htmlContent = templateEngine.process("success", context);

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(htmlContent);
    }

    @GetMapping("/payments/fail")
    public ResponseEntity<String> paymentFail(@RequestParam String paymentKey) {
        PaymentResponse.Get response = paymentService.paymentFail(paymentKey);

        Context context = new Context();
        context.setVariable("orderId", response.getOrderId());
        context.setVariable("orderName", response.getOrderName());
        context.setVariable("amount", response.getAmount());
        context.setVariable("createdAt", response.getCreatedAt());

        String htmlContent = templateEngine.process("fail", context);

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(htmlContent);
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
