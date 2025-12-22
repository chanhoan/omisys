package com.omisys.promotion.server.domain.model.vo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.omisys.promotion.server.exception.PromotionErrorCode;
import com.omisys.promotion.server.exception.PromotionException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.Arrays;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum CouponType {
    TIER("TIER"),
    EVENT("EVENT");

    private String type;

    @JsonValue
    public String getType() {
        return this.type;
    }

    @JsonCreator
    public static CouponType from(String type) {
        return Arrays.stream(CouponType.values())
                .filter(t -> t.getType().equals(type))
                .findFirst()
                .orElseThrow(() -> new PromotionException(PromotionErrorCode.INVALID_COUPON_TYPE));
    }
}