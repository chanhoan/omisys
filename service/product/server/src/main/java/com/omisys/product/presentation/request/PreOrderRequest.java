package com.omisys.product.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

public class PreOrderRequest {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Create{

        @NotNull(message = "상품아이디는 필수입니다")
        private UUID productId;

        @NotBlank(message = "사전예약주문타이틀은 필수입니다")
        private String preOrderTitle;

        @NotNull(message = "사전예약시작일자는 필수입니다")
        private LocalDateTime startDateTime;

        @NotNull(message = "사전예약종료일자는 필수입니다")
        private LocalDateTime endDateTime;

        @NotNull(message = "사전예약발송일자는 필수입니다")
        private LocalDateTime releaseDateTime;

        @NotNull(message = "사전예약수량은 필수입니다")
        private Integer availableQuantity;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Update{

        @NotNull(message = "사전예약상품아이디는 필수입니다")
        private Long preOrderId;
        
        @NotNull(message = "상품아이디는 필수입니다")
        private UUID productId;
        
        @NotBlank(message = "사전예약주문타이틀은 필수입니다")
        private String preOrderTitle;
        
        @NotNull(message = "사전예약시작일자는 필수입니다")
        private LocalDateTime startDateTime;
        
        @NotNull(message = "사전예약종료일자는 필수입니다")
        private LocalDateTime endDateTime;
        
        @NotNull(message = "사전예약발송일자는 필수입니다")
        private LocalDateTime releaseDateTime;
        
        @NotNull(message = "사전예약수량은 필수입니다")
        private Integer availableQuantity;
    }

}
