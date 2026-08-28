package com.omisys.product.application.product;

import com.omisys.product.application.category.CategoryService;
import com.omisys.product.application.dto.ImgDto;
import com.omisys.product.domain.model.Product;
import com.omisys.product.exception.ProductErrorCode;
import com.omisys.product.exception.ProductException;
import com.omisys.product.presentation.request.ProductRequest;
import com.omisys.product.presentation.response.ProductResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "ProductFacadeService")
public class ProductFacadeService {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final ElasticSearchService elasticSearchService;
    private final S3ImageService imageService;

    @Transactional
    public String createProduct(
        ProductRequest.Create request,
        MultipartFile productImg,
        MultipartFile detailImg) throws IOException {
        validateCategoryId(request.getCategoryId());
        String productImgUrl = imageService.uploadImage("origin", productImg);
        String detailImgUrl = imageService.uploadImage("detail", detailImg);
        // 리사이즈 파이프라인이 없어 썸네일은 원본과 같은 이미지를 가리킨다.
        // 별도 크기가 필요해지면 이 값만 바꾸면 되고, 저장 구조는 그대로 둘 수 있다.
        String thumbnailImgUrl = productImgUrl;
        ProductResponse productResponse =
                productService.createProduct(
                        request, new ImgDto(productImgUrl, detailImgUrl, thumbnailImgUrl));
        elasticSearchService.saveProduct(productResponse);
        return productResponse.getProductId();
    }

    @Transactional
    public ProductResponse updateProduct(
            ProductRequest.Update request,
            MultipartFile productImg,
            MultipartFile detailImg) throws IOException {
        validateCategoryId(request.getCategoryId());
        Product product = productService.getSavedProduct(request.getProductId());
        ImgDto imgData = fetchImgUrls(product, productImg, detailImg);
        ProductResponse newProduct = productService.updateProduct(request, product, imgData);
        elasticSearchService.updateProduct(newProduct);
        return newProduct;
    }

    @Transactional
    public ProductResponse updateStatus(UUID productId, boolean status) {
        ProductResponse product = productService.updateStatus(productId, status);
        elasticSearchService.updateProduct(product);
        return product;
    }

    @Transactional
    public boolean deleteProduct(UUID productId) {
        ProductResponse product = productService.deleteProduct(productId);
        elasticSearchService.deleteProduct(product);
        // 썸네일은 원본과 같은 객체를 가리키므로 따로 지우지 않는다.
        Optional.ofNullable(product.getOriginImgUrl()).ifPresent(imageService::deleteImage);
        Optional.ofNullable(product.getDetailImgUrl()).ifPresent(imageService::deleteImage);
        return product.isDeleted();
    }

    private ImgDto fetchImgUrls(
            Product savedProduct,
            MultipartFile productImg,
            MultipartFile detailImg) throws IOException {
        String productImgUrl = savedProduct.getOriginImgUrl();
        String detailImgUrl = savedProduct.getDetailImgUrl();
        String thumbnailImgUrl = savedProduct.getThumbnailImgUrl();
        if (productImg != null && !productImg.isEmpty()) {
            imageService.deleteImage(productImgUrl);
            productImgUrl = imageService.uploadImage("origin", productImg);
            thumbnailImgUrl = productImgUrl;
        }
        if (detailImg != null && !detailImg.isEmpty()) {
            imageService.deleteImage(detailImgUrl);
            detailImgUrl = imageService.uploadImage("detail", detailImg);
        }
        return new ImgDto(productImgUrl, detailImgUrl, thumbnailImgUrl);
    }

    private void validateCategoryId(Long categoryId) {
        if (!categoryService.existsCategory(categoryId)) {
            throw new ProductException(ProductErrorCode.NOT_FOUND_CATEGORY);
        }
    }

}
