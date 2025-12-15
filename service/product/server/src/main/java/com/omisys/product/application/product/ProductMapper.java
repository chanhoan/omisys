package com.omisys.product.application.product;

import com.omisys.product.application.dto.ImgDto;
import com.omisys.product.domain.model.Product;
import com.omisys.product.presentation.request.ProductRequest;
import com.omisys.product.product_dto.ProductDto;

public class ProductMapper {

    public static ProductDto fromEntity(Product product) {
        return ProductDto.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .originalPrice(product.getOriginalPrice())
                .discountPercent(product.getDiscountPercent())
                .discountedPrice(product.getDiscountedPrice())
                .stock(product.getStock())
                .tags(product.getTags())
                .build();
    }

    public static Product toEntity(ProductRequest.Create request, ImgDto imgDto) {
        return Product.builder()
                .categoryId(request.getCategoryId())
                .productName(request.getProductName())
                .brandName(request.getBrandName())
                .mainColor(request.getMainColor())
                .size(request.getSize())
                .description(request.getDescription())
                .originalPrice(request.getOriginalPrice())
                .discountPercent(request.getDiscountPercent())
                .originImgUrl(imgDto.originImgUrl())
                .detailImgUrl(imgDto.detailImgUrl())
                .thumbnailImgUrl(imgDto.thumbnailImgUrl())
                .stock(request.getStock())
                .limitCountPerUser(request.getLimitCountPerUser())
                .tags(request.getTags())
                .build();
    }

    public static void updateProduct(
            ProductRequest.Update request, Product existingProduct, ImgDto imgUrls) {
        existingProduct.updateProduct(
                request.getCategoryId(),
                request.getProductName(),
                request.getBrandName(),
                request.getMainColor(),
                request.getSize(),
                request.getOriginalPrice(),
                request.getDiscountPercent(),
                request.getStock(),
                request.getDescription(),
                imgUrls.originImgUrl(),
                imgUrls.detailImgUrl(),
                imgUrls.thumbnailImgUrl(),
                request.getLimitCountPerUser(),
                request.getTags(),
                request.getIsPublic());
    }
}
