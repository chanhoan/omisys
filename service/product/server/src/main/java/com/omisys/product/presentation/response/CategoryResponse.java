package com.omisys.product.presentation.response;

import com.omisys.product.domain.model.Category;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
public class CategoryResponse {

    private Long categoryId;
    private String name;
    private List<CategoryResponse> subCategories;

    public CategoryResponse(Long categoryId, String name, List<CategoryResponse> subCategories) {
        this.categoryId = categoryId;
        this.name = name;
        this.subCategories = subCategories;
    }

    public static CategoryResponse fromEntity(Category category) {
        return new CategoryResponse(
                category.getCategoryId(),
                category.getName(),
                category.getSubCategories().stream()
                        .map(CategoryResponse::fromEntity)
                        .collect(Collectors.toList()));
    }
}
