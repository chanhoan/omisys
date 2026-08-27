package com.omisys.product.infrastructure.utils;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.omisys.product.domain.model.PreOrder;
import com.omisys.product.exception.ProductErrorCode;
import com.omisys.product.exception.ProductException;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Slf4j
public record PreOrderRedisDto(
        Long preOrderId,
        String productId,
        Integer availableQuantity,
        @JsonSerialize(using = LocalDateTimeSerializer.class)
        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        LocalDateTime startDateTime,
        @JsonSerialize(using = LocalDateTimeSerializer.class)
        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        LocalDateTime endDateTime) {

    public PreOrderRedisDto(PreOrder preOrder) {
        this(
                preOrder.getPreOrderId(),
                preOrder.getProductId().toString(),
                preOrder.getAvailableQuantity(),
                preOrder.getStartDateTime(),
                preOrder.getEndDateTime());
    }

    public void validateReservationDate() {
        if (!isReservation()) {
            throw new ProductException(ProductErrorCode.INVALID_PREORDER_DATETIME);
        }
    }

    private boolean isReservation() {
        LocalDateTime now = LocalDateTime.now();
        return startDateTime.isBefore(now) && endDateTime.isAfter(now);
    }
}
