package com.omisys.product.application.product;

import com.omisys.product.application.dto.ImgDto;
import com.omisys.product.domain.model.Product;
import com.omisys.product.domain.model.SortOption;
import com.omisys.product.domain.repository.cassandra.ProductRepository;
import com.omisys.product.exception.ProductErrorCode;
import com.omisys.product.exception.ProductException;
import com.omisys.product.presentation.request.ProductRequest;
import com.omisys.product.presentation.response.ProductResponse;
import com.omisys.product.product_dto.ProductDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "ProductService")
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public ProductResponse createProduct(ProductRequest.Create request, ImgDto imgDto) {
        Product newProduct = ProductMapper.toEntity(request, imgDto);
        newProduct.setNew(true);
        Product savedProduct = productRepository.save(newProduct);
        return ProductResponse.fromEntity(savedProduct);
    }

    @Transactional
    public ProductResponse updateProduct(
            ProductRequest.Update request,
            Product savedProduct,
            ImgDto imgDto) {
        ProductMapper.updateProduct(request, savedProduct, imgDto);
        return ProductResponse.fromEntity(savedProduct);
    }

    @Transactional
    public ProductResponse updateStatus(UUID productId, boolean status) {
        Product product = getSavedProduct(productId);
        product.setSoldout(status);
        productRepository.save(product);
        return ProductResponse.fromEntity(product);
    }

    @Transactional
    public ProductResponse deleteProduct(UUID productId) {
        Product product = getSavedProduct(productId);
        product.isDelete();
        productRepository.save(product);
        return ProductResponse.fromEntity(product);
    }

    @Transactional
    public void reduceStock(Map<String, Integer> productQuantities) {
        log.info(productQuantities.toString());
        productQuantities.forEach((productId, value) -> {
            log.info(productId, value);
            int reduceCount = value;
            Product product = getSavedProduct(UUID.fromString(productId));
            validateProductStock(product, reduceCount);
            product.updateStock(reduceCount);
            productRepository.save(product);
        });
    }

    @Transactional
    public void rollbackStock(Map<String, Integer> productQuantities) {
        productQuantities.forEach((productId, value) -> {
            int rollbackCount = value;
            Product product = getSavedProduct(UUID.fromString(productId));
            product.rollbackStock(rollbackCount);
            productRepository.save(product);
        });
    }

    public ProductResponse getProduct(UUID productId) {
        return ProductResponse.fromEntity(getSavedProduct(productId));
    }

    public Page<ProductResponse> getProductList(
            int page,
            int size,
            Long categoryId,
            String brandName,
            Long minPrice,
            Long maxPrice,
            String productSize,
            String mainColor,
            String sortOption) {
        SortOption sort = SortOption.valueOf(sortOption.toUpperCase());
        Sort.Direction direction =
                sort.getOrder().name().contains("Asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort.getField()));
        List<ProductResponse> result =
                productRepository
                        .findAllByFilters(
                                categoryId,
                                brandName,
                                BigDecimal.valueOf(minPrice),
                                BigDecimal.valueOf(maxPrice),
                                productSize,
                                mainColor,
                                pageable)
                        .stream()
                        .map(ProductResponse::fromEntity)
                        .toList();
        return new PageImpl<>(result, pageable, result.size());
    }

    public List<ProductDto> getProductList(List<String> productIds) {
        return productIds.stream()
                .map(productId -> getSavedProduct(UUID.fromString(productId)))
                .map(ProductMapper::fromEntity)
                .toList();
    }

    public Product getSavedProduct(UUID productId) {
        return productRepository
                .findByProductIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new ProductException(ProductErrorCode.NOT_FOUND_PRODUCT));
    }

    private void validateProductStock(Product product, int reduceCount) {
        if (product.getStock() < reduceCount) {
            throw new ProductException(ProductErrorCode.STOCK_NOT_AVAILABLE);
        }
    }
}
