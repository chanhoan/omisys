package com.omisys.order.order_dto.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderProductDto {

    private Long orderProductId;
    private Long orderId;
    private UUID productId;
    private Integer quantity;
    private int purchasePrice;

}
