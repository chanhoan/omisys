package com.omisys.order.order_dto.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderProductInfo {

    private String productId;
    private int quantity;
    private Long userCouponId;

}
