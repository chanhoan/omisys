package com.omisys.order.server.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder(access = AccessLevel.PROTECTED)
@Table(name = "P_ORDER_PRODUCT")
public class OrderProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderProductId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private String productId;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private BigDecimal purchasePrice;

    @Column
    private Long userCouponId;

    @Column
    private BigDecimal couponPrice;

    public static OrderProduct createOrderProduct(String productId, BigDecimal productPrice,
                                                  String productName,
                                                  int quantity, Long userCouponId, Order order) {

        return OrderProduct.builder()
                .order(order)
                .productId(productId)
                .quantity(quantity)
                .purchasePrice(
                        userCouponId != null ? productPrice.multiply(BigDecimal.valueOf(quantity))
                                .subtract(BigDecimal.valueOf(100))
                                : productPrice.multiply(BigDecimal.valueOf(quantity)))
                .productName(productName)
                .userCouponId(userCouponId)
                .couponPrice(userCouponId != null ? BigDecimal.valueOf(100) : BigDecimal.ZERO)
                .build();
    }

}
