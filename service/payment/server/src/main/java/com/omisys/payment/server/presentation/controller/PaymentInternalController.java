package com.omisys.payment.server.presentation.controller;

import com.omisys.payment.payment_dto.dto.PaymentInternalDto;
import com.omisys.payment.server.application.service.PaymentInternalService;
import com.omisys.payment.server.presentation.request.PaymentRequest;
import com.omisys.payment.server.presentation.response.PaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/payments")
@RequiredArgsConstructor
public class PaymentInternalController {

    private final PaymentInternalService paymentInternalService;

    @PostMapping("")
    public PaymentInternalDto.Created createPayment(@RequestBody PaymentRequest.Create createRequest) {
        String checkoutUrl = paymentInternalService.createPayment(createRequest);
        return new PaymentInternalDto.Created(checkoutUrl);
    }

    @PostMapping("/cancel")
    public void cancelPayment(@RequestBody PaymentRequest.Cancel cancelRequest) {
        paymentInternalService.cancelPayment(cancelRequest);
    }

    @GetMapping("/{orderId}")
    public PaymentResponse.Get getPaymentAndHistoryByOrderId(@PathVariable Long orderId) {
        return paymentInternalService.getPaymentAndHistoryByOrderId(orderId);
    }

}
