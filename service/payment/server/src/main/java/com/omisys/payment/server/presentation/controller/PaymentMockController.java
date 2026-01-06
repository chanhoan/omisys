package com.omisys.payment.server.presentation.controller;

import com.omisys.payment.server.application.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Profile({"local", "test"})
@RestController
@RequiredArgsConstructor
public class PaymentMockController {

    private final PaymentService paymentService;

    @PostMapping("/payments/mock/success")
    public ResponseEntity<Void> paymentSuccessMock(@RequestParam String paymentKey) {
        paymentService.paymentSuccessMock(paymentKey);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/payments/mock/keys")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<List<String>> latestPaymentKeys(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(paymentService.findLatestKeys(limit));
    }
}
