package com.omisys.product.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ProductRequest {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Create{

        @NotNull(message = "카테고리아이디는 필수입니다")
        private Long categoryId;

        @NotBlank(message = "상품이름은 필수입니다")
        private String productName;

        @NotBlank(message = "브랜드이름은 필수입니다")
        private String brandName;

        @NotBlank(message = "메인컬러는 필수입니다")
        private String mainColor;

        @NotBlank(message = "상품사이즈는 필수입니다")
        private String size;

        @NotNull(message = "상품가격은 필수입니다")
        private BigDecimal originalPrice;

        private Double discountPercent;

        @NotNull(message = "상품재고수량은 필수입니다")
        private Integer stock;

        @NotBlank(message = "상품설명은 필수입니다")
        private String description;

        @NotNull(message = "상품이름은 필수입니다")
        private Integer limitCountPerUser;

        private List<String> tags;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Update{

        @NotNull(message = "상품아이디는 필수입니다")
        private UUID productId;

        @NotNull(message = "카테고리아이디는 필수입니다")
        private Long categoryId;

        @NotBlank(message = "상품이름은 필수입니다")
        private String productName;

        @NotBlank(message = "브랜드이름은 필수입니다")
        private String brandName;

        @NotBlank(message = "메인컬러는 필수입니다")
        private String mainColor;

        @NotBlank(message = "상품사이즈는 필수입니다")
        private String size;

        @NotNull(message = "상품가격은 필수입니다")
        private BigDecimal originalPrice;

        private Double discountPercent;

        @NotNull(message = "상품재고수량은 필수입니다")
        private Integer stock;

        @NotBlank(message = "상품설명은 필수입니다")
        private String description;

        @NotNull(message = "상품이름은 필수입니다")
        private Integer limitCountPerUser;

        @NotNull(message = "공개여부는 필수입니다")
        private Boolean isPublic;

        private List<String> tags;
    }

}
