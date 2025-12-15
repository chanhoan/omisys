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
        String thumbnailImgUrl = getThumbnailImgUrl(detailImgUrl);
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
        Optional.ofNullable(product.getOriginImgUrl()).ifPresent(imageService::deleteImage);
        Optional.ofNullable(product.getDetailImgUrl()).ifPresent(imageService::deleteImage);
        Optional.ofNullable(product.getThumbnailImgUrl()).ifPresent(imageService::deleteImage);
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
            imageService.deleteImage(thumbnailImgUrl);
            productImgUrl = imageService.uploadImage("origin", productImg);
            thumbnailImgUrl = getThumbnailImgUrl(productImgUrl);
        }
        if (detailImg != null && !detailImg.isEmpty()) {
            imageService.deleteImage(detailImgUrl);
            detailImgUrl = imageService.uploadImage("detail", detailImg);
        }
        return new ImgDto(productImgUrl, detailImgUrl, thumbnailImgUrl);
    }

    private String getThumbnailImgUrl(String productImgUrl) {
        String[] parts = productImgUrl.split("/");
        String fileNameWithExtension = parts[parts.length - 1];

        String imageName = "resized-" + fileNameWithExtension;

        String baseUrl =
                productImgUrl
                        .substring(0, productImgUrl.lastIndexOf("/") + 1)
                        .replace("omisys-products-origin", "omisys-products-thumbnail");
        return baseUrl + imageName;
    }

    private void validateCategoryId(Long categoryId) {
        if (!categoryService.existsCategory(categoryId)) {
            throw new ProductException(ProductErrorCode.NOT_FOUND_CATEGORY);
        }
    }

}
