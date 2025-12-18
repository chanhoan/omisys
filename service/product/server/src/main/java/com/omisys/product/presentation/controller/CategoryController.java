package com.omisys.product.presentation.controller;

import com.omisys.common.domain.response.ApiResponse;
import com.omisys.product.application.category.CategoryService;
import com.omisys.product.presentation.request.CategoryRequest;
import com.omisys.product.presentation.response.CategoryResponse;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Validated
public class CategoryController {

    private final CategoryService categoryService;

    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER')")
    @PostMapping
    public ApiResponse<Long> createCategory(@RequestBody @Validated CategoryRequest.Create request) {
        return ApiResponse.created(
                categoryService.createCategory(request.getName(), request.getParentCategoryId()));
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER')")
    @PatchMapping("/{categoryId}")
    public ApiResponse<CategoryResponse> updateCategory(
            @PathVariable("categoryId") @NotNull Long categoryId,
            @RequestBody @Validated CategoryRequest.Update request) {
        return ApiResponse.ok(
                categoryService.updateCategory(categoryId, request.getName(), request.getParentCategoryId()));
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER')")
    @DeleteMapping("/{categoryId}")
    public ApiResponse<Void> deleteCategory(@PathVariable("categoryId") @NotNull Long categoryId) {
        categoryService.deleteCategory(categoryId);
        return ApiResponse.ok();
    }

    @GetMapping("/search")
    public ApiResponse<List<CategoryResponse>> getCategories() {
        return ApiResponse.ok(categoryService.fetchAndCacheCategories());
    }
}
