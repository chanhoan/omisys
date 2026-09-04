package com.omisys.payment.server.presentation.controller;

import com.omisys.payment.server.application.service.PaymentService;
import com.omisys.payment.server.presentation.response.PaymentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock private PaymentService paymentService;

    private PaymentController paymentController;

    @BeforeEach
    void setUp() {
        paymentController = new PaymentController(paymentService);
        ReflectionTestUtils.setField(paymentController, "frontendBaseUrl", "http://localhost:3000/");
    }

    @Test
    void paymentSuccess_confirmsPaymentThenRedirectsToFrontendResult() {
        when(paymentService.paymentSuccess("payment-key"))
                .thenReturn(PaymentResponse.Get.builder().orderId(20L).build());

        var response = paymentController.paymentSuccess("payment-key");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation())
                .hasToString("http://localhost:3000/checkout/result?status=success&orderId=20");
    }

    @Test
    void paymentFail_redirectsToFrontendResultWithoutPaymentKey() {
        var response = paymentController.paymentFail("PAY_PROCESS_CANCELED", "결제가 취소되었습니다.", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation().getPath()).isEqualTo("/checkout/result");
        assertThat(response.getHeaders().getLocation().getQuery())
                .isEqualTo("status=fail&reason=결제가 취소되었습니다.");
        verifyNoInteractions(paymentService);
    }
}
