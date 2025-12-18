package com.omisys.order.server.presentation.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CartProductRequest {

    @NotNull(message = "상품 ID는 필수 값입니다.")
    private String productId;

    @NotNull(message = "상품 수량은 필수 값입니다.")
    @Min(value = 1, message = "상품 수량 최소 값은 1 입니다.")
    private Integer quantity;

}
