package com.omisys.product.application.category;

import com.omisys.product.domain.model.Category;
import com.omisys.product.domain.repository.jpa.CategoryRepository;
import com.omisys.product.exception.ProductErrorCode;
import com.omisys.product.exception.ProductException;
import com.omisys.product.presentation.response.CategoryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j(topic = "CategoryService")
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    @CacheEvict(cacheNames = "categories-cache", key = "'categories'")
    public Long createCategory(String name, Long parentCategoryId) {
        Category parent =
                Optional.ofNullable(parentCategoryId).map(this::findByCategoryId).orElse(null);
        Category category = new Category(name, parent);
        Optional.ofNullable(parent).ifPresent(p -> p.addSubCategory(category));

        var saved = categoryRepository.save(category);
        return saved.getCategoryId();
    }

    @Transactional
    @CacheEvict(cacheNames = "categories-cache", key = "'categories'")
    public CategoryResponse updateCategory(
            Long targetCategoryId, String name, Long parentCategoryId) {
        Category target = findByCategoryId(targetCategoryId);
        Category parent =
                Optional.ofNullable(parentCategoryId).map(this::findByCategoryId).orElse(null);

        target.update(name, parent);
        syncParentCategory(target, parent);
        return CategoryResponse.fromEntity(target);
    }

    @Transactional
    @CacheEvict(cacheNames = "categories-cache", key = "'categories'")
    public void deleteCategory(Long categoryId) {
        Category category = findByCategoryId(categoryId);
        categoryRepository.delete(category);
    }

    @Cacheable(cacheNames = "categories-cache", key = "'categories'")
    public List<CategoryResponse> fetchAndCacheCategories() {
        return categoryRepository.findAllWithSubCategories().stream()
                .filter(category -> category.getParent() == null)
                .map(CategoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public boolean existsCategory(Long categoryId) {
        return categoryRepository.existsByCategoryId(categoryId);
    }

    private void syncParentCategory(Category target, Category newParent) {
        Category oldParent = target.getParent();
        if (oldParent != null) {
            oldParent.removeSubCategory(newParent);
        }
        if (newParent != null) {
            newParent.addSubCategory(target);
        }
    }

    private Category findByCategoryId(Long categoryId) {
        return categoryRepository
                .findByCategoryId(categoryId)
                .orElseThrow(() -> new ProductException(ProductErrorCode.NOT_FOUND_CATEGORY));
    }

}
