package com.omisys.user.application.dto;

import com.omisys.user.domain.model.Tier;
import com.omisys.user.domain.model.User;
import com.omisys.user.domain.model.UserTier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class UserTierResponse {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Get {

        private Long userId;
        private String username;
        private String tier;

        public static Get of(User user, Tier tier) {

            return Get.builder()
                    .userId(user.getId())
                    .username(user.getUsername())
                    .tier(tier.getName())
                    .build();

        }
    }
}
