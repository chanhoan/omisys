package com.omisys.order.order_dto.dto;

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

    private String orderType;
    private List<OrderProductInfo> orderProductInfos = new ArrayList<>();
    private BigDecimal pointPrice;
    private Long addressId;

}
