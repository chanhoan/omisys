package com.omisys.user.application.dto;

import com.omisys.user.domain.model.PointHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

public class PointResponse {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Get {

        private Long pointHistoryId;
        private Long userId;
        private Long orderId;
        private BigDecimal point;
        private String type;

        public static PointResponse.Get of(PointHistory pointHistory) {

            return Get.builder()
                    .pointHistoryId(pointHistory.getId())
                    .userId(pointHistory.getUser().getId())
                    .orderId(pointHistory.getOrderId())
                    .point(pointHistory.getPoint())
                    .type(pointHistory.getType().name())
                    .build();

        }
    }
}
