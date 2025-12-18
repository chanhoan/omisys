package com.omisys.order.server.presentation.response;

import com.omisys.product.product_dto.ProductDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CartProductResponse {

    private String productId;
    private Integer quantity;

    private String name;
    private BigDecimal originalPrice;
    private BigDecimal discountedPrice;
    private Double discountPercent;

    public static CartProductResponse from(ProductDto product, Integer quantity) {
        return new CartProductResponse(
                product.getProductId().toString(),
                quantity,
                product.getProductName(),
                product.getOriginalPrice(),
                product.getDiscountedPrice(),
                product.getDiscountPercent());
    }

}
