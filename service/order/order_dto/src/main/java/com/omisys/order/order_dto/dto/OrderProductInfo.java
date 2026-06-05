package com.omisys.order.order_dto.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderProductInfo {

    @NotBlank(message = "상품 ID는 필수입니다.")
    private String productId;

    @Min(value = 1, message = "주문 수량은 최소 1개 이상이어야 합니다.")
    private int quantity;

    private Long userCouponId;

}
