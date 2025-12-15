package com.omisys.product.product_dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class ProductDto {

    private UUID productId;
    private String productName;
    private BigDecimal originalPrice;
    private BigDecimal discountedPrice;
    private Double discountPercent;
    private int stock;
    private List<String> tags;

    @Builder
    public ProductDto(
            UUID productId,
            String productName,
            BigDecimal originalPrice,
            BigDecimal discountedPrice,
            Double discountPercent,
            int stock,
            List<String> tags,
            boolean isCoupon) {
        this.productId = productId;
        this.productName = productName;
        this.originalPrice = originalPrice;
        this.discountedPrice = discountedPrice;
        this.discountPercent = discountPercent;
        this.stock = stock;
        this.tags = tags;
    }
}
