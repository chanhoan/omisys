package com.omisys.review.server.presentation.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

public class ReviewRequest {

    @Getter
    @Builder
    public static class Create {
        @NotBlank
        private String productId;

        @NotNull
        private Long orderId;

        @NotNull
        @Min(1) @Max(5)
        private Integer rating;

        @NotBlank
        @Size(max = 1000)
        private String content;
    }

    @Getter
    @Builder
    public static class Update {
        @NotNull
        @Min(1) @Max(5)
        private Integer rating;

        @NotBlank
        @Size(max = 1000)
        private String content;
    }
}
