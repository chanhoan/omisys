package com.omisys.product.presentation.response;

import com.omisys.product.domain.model.PreOrder;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class PreOrderResponse {

    Long preOrderId;
    UUID productId;
    String preOrderTitle;
    String state;
    LocalDateTime startDateTime;
    LocalDateTime endDateTime;
    LocalDateTime releaseDateTime;
    Integer availableQuantity;
    boolean isPublic;
    boolean isDeleted;

    @Builder
    public PreOrderResponse(
            Long preOrderId,
            UUID productId,
            String preOrderTitle,
            String state,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            LocalDateTime releaseDateTime,
            Integer availableQuantity,
            boolean isPublic,
            boolean isDeleted) {
        this.preOrderId = preOrderId;
        this.productId = productId;
        this.preOrderTitle = preOrderTitle;
        this.state = state;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.releaseDateTime = releaseDateTime;
        this.availableQuantity = availableQuantity;
        this.isPublic = isPublic;
        this.isDeleted = isDeleted;
    }

    public static PreOrderResponse of(PreOrder preOrder) {
        return PreOrderResponse.builder()
                .preOrderId(preOrder.getPreOrderId())
                .productId(preOrder.getProductId())
                .preOrderTitle(preOrder.getPreOrderTitle())
                .state(preOrder.getState().name())
                .startDateTime(preOrder.getStartDateTime())
                .endDateTime(preOrder.getEndDateTime())
                .releaseDateTime(preOrder.getReleaseDateTime())
                .availableQuantity(preOrder.getAvailableQuantity())
                .isPublic(preOrder.isPublic())
                .isDeleted(preOrder.isDeleted())
                .build();
    }

}
