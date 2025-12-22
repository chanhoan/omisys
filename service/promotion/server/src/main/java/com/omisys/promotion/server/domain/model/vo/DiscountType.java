package com.omisys.promotion.server.domain.model.vo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.omisys.promotion.server.exception.PromotionErrorCode;
import com.omisys.promotion.server.exception.PromotionException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.Arrays;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum DiscountType {
    PRICE("PRICE"),
    PERCENTAGE("PERCENTAGE");

    private String discountType;

    @JsonValue
    public String getDiscountType() {
        return this.discountType;
    }

    @JsonCreator
    public static DiscountType from(String type) {
        return Arrays.stream(DiscountType.values())
                .filter(t -> t.getDiscountType().equals(type))
                .findFirst()
                .orElseThrow(() -> new PromotionException(PromotionErrorCode.INVALID_DISCOUNT_TYPE));
    }
}
