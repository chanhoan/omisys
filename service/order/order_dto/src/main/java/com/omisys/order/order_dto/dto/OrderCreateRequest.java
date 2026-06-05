package com.omisys.order.order_dto.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateRequest {

    @NotBlank(message = "주문 유형은 필수입니다.")
    private String orderType;

    @NotEmpty(message = "주문 상품은 최소 1개 이상이어야 합니다.")
    @Valid
    private List<OrderProductInfo> orderProductInfos = new ArrayList<>();

    @PositiveOrZero(message = "포인트 사용 금액은 0 이상이어야 합니다.")
    private BigDecimal pointPrice;

    @NotNull(message = "배송지 ID는 필수입니다.")
    private Long addressId;

}
