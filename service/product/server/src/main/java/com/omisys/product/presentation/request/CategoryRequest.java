package com.omisys.product.presentation.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class CategoryRequest {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Create {

        @NotBlank(message = "카테고리명은 필수입니다")
        private String name;

        private Long parentCategoryId;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Update {

        @NotBlank(message = "카테고리명은 필수입니다")
        private String name;

        private Long parentCategoryId;
    }

}
