package com.omisys.user.application.dto;

import com.omisys.user.domain.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

public class UserResponse {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Info {

        private Long userId;
        private String username;
        private String role;
        private BigDecimal point;

        public static Info of(User user) {

            return Info.builder()
                    .userId(user.getId())
                    .username(user.getUsername())
                    .role(user.getRole().name())
                    .point(user.getPoint())
                    .build();

        }
    }
}
