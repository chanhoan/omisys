package com.omisys.product.application.product;

import com.omisys.product.application.category.CategoryService;
import com.omisys.product.application.dto.ImgDto;
import com.omisys.product.domain.model.Product;
import com.omisys.product.exception.ProductErrorCode;
import com.omisys.product.exception.ProductException;
import com.omisys.product.presentation.request.ProductRequest;
import com.omisys.product.presentation.response.ProductResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductFacadeServiceTest {

    @Mock private ProductService productService;
    @Mock private CategoryService categoryService;
    @Mock private ElasticSearchService elasticSearchService;
    @Mock private S3ImageService imageService;

    @InjectMocks
    private ProductFacadeService productFacadeService;

    @Test
    @DisplayName("createProduct: 카테고리 검증 → 이미지 업로드 → 상품 생성 → ES 저장 → productId 반환")
    void createProduct_success() throws IOException {
        // given
        ProductRequest.Create request = new ProductRequest.Create(
                10L,
                "AirMax",
                "NIKE",
                "BLACK",
                "270",
                BigDecimal.valueOf(200_000),
                10.0,
                100,
                "nice shoes",
                1,
                List.of("shoes")
        );

        MultipartFile productImg = mock(MultipartFile.class);
        MultipartFile detailImg = mock(MultipartFile.class);

        when(categoryService.existsCategory(10L)).thenReturn(true);

        // 썸네일 생성 로직이 "omisys-products-origin" -> "omisys-products-thumbnail" replace를 사용함
        String originUrl = "https://s3.amazonaws.com/omisys-products-origin/origin.jpg";
        String detailUrl = "https://s3.amazonaws.com/omisys-products-origin/detail.jpg";

        when(imageService.uploadImage(eq("origin"), eq(productImg))).thenReturn(originUrl);
        when(imageService.uploadImage(eq("detail"), eq(detailImg))).thenReturn(detailUrl);

        ProductResponse mockedResponse = mock(ProductResponse.class);
        when(mockedResponse.getProductId()).thenReturn(UUID.randomUUID().toString());

        // productService.createProduct 호출 시 ImgDto가 올바르게 들어가는지 캡처해서 검증
        when(productService.createProduct(eq(request), any(ImgDto.class))).thenReturn(mockedResponse);

        // when
        String productId = productFacadeService.createProduct(request, productImg, detailImg);

        // then
        assertThat(productId).isEqualTo(mockedResponse.getProductId());

        ArgumentCaptor<ImgDto> imgCaptor = ArgumentCaptor.forClass(ImgDto.class);
        verify(productService).createProduct(eq(request), imgCaptor.capture());

        ImgDto usedImg = imgCaptor.getValue();
        assertThat(usedImg.originImgUrl()).isEqualTo(originUrl);
        assertThat(usedImg.detailImgUrl()).isEqualTo(detailUrl);

        // detailUrl 기준으로 thumbnail을 만든다(현재 코드 기준)
        assertThat(usedImg.thumbnailImgUrl())
                .isEqualTo("https://s3.amazonaws.com/omisys-products-thumbnail/resized-detail.jpg");

        verify(elasticSearchService).saveProduct(mockedResponse);
    }

    @Test
    @DisplayName("createProduct: 카테고리 미존재 → NOT_FOUND_CATEGORY 예외")
    void createProduct_categoryNotFound_throws() {
        // given
        ProductRequest.Create request = new ProductRequest.Create(
                999L, "n", "b", "c", "s",
                BigDecimal.valueOf(1), null, 1, "d", 1, List.of()
        );

        when(categoryService.existsCategory(999L)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> productFacadeService.createProduct(request, mock(MultipartFile.class), mock(MultipartFile.class)))
                .isInstanceOf(ProductException.class)
                .extracting(ex -> ((ProductException) ex).getErrorCode())
                .isEqualTo(ProductErrorCode.NOT_FOUND_CATEGORY);

        verifyNoInteractions(imageService);
        verifyNoInteractions(productService);
        verifyNoInteractions(elasticSearchService);
    }

    @Test
    @DisplayName("updateProduct: 카테고리 검증 → 기존 상품 조회 → 이미지 갱신 판단 → 상품 업데이트 → ES 업데이트")
    void updateProduct_success_noImageChange() throws IOException {
        // given
        UUID productId = UUID.randomUUID();
        ProductRequest.Update request = new ProductRequest.Update(
                productId,
                10L,
                "newName",
                "NIKE",
                "BLACK",
                "270",
                BigDecimal.valueOf(200_000),
                10.0,
                100,
                "desc",
                1,
                true,
                List.of("tag")
        );

        when(categoryService.existsCategory(10L)).thenReturn(true);

        Product saved = Product.builder()
                .categoryId(10L)
                .productName("old")
                .brandName("NIKE")
                .mainColor("BLACK")
                .size("270")
                .originalPrice(BigDecimal.valueOf(1000))
                .discountPercent(null)
                .stock(10)
                .description("d")
                .originImgUrl("origin-old")
                .detailImgUrl("detail-old")
                .thumbnailImgUrl("thumb-old")
                .limitCountPerUser(1)
                .tags(List.of("t"))
                .build();

        when(productService.getSavedProduct(productId)).thenReturn(saved);

        ProductResponse mockedResponse = mock(ProductResponse.class);
        when(productService.updateProduct(eq(request), eq(saved), any(ImgDto.class))).thenReturn(mockedResponse);

        // when: productImg/detailImg null -> 기존 url 그대로 사용해야 함
        ProductResponse result = productFacadeService.updateProduct(request, null, null);

        // then
        assertThat(result).isSameAs(mockedResponse);

        ArgumentCaptor<ImgDto> imgCaptor = ArgumentCaptor.forClass(ImgDto.class);
        verify(productService).updateProduct(eq(request), eq(saved), imgCaptor.capture());

        ImgDto used = imgCaptor.getValue();
        assertThat(used.originImgUrl()).isEqualTo("origin-old");
        assertThat(used.detailImgUrl()).isEqualTo("detail-old");
        assertThat(used.thumbnailImgUrl()).isEqualTo("thumb-old");

        verify(elasticSearchService).updateProduct(mockedResponse);

        // 이미지가 바뀌지 않았으므로 delete/upload 호출이 없어야 한다.
        verify(imageService, never()).deleteImage(anyString());
        verify(imageService, never()).uploadImage(anyString(), any());
    }

    @Test
    @DisplayName("deleteProduct: ES 삭제 → 이미지 삭제(널 아닌 것만) → 삭제 여부 반환")
    void deleteProduct_success() {
        // given
        UUID productId = UUID.randomUUID();

        ProductResponse mocked = mock(ProductResponse.class);
        when(mocked.getOriginImgUrl()).thenReturn("origin-url");
        when(mocked.getDetailImgUrl()).thenReturn("detail-url");
        when(mocked.getThumbnailImgUrl()).thenReturn("thumb-url");
        when(mocked.isDeleted()).thenReturn(true);

        when(productService.deleteProduct(productId)).thenReturn(mocked);

        // when
        boolean deleted = productFacadeService.deleteProduct(productId);

        // then
        assertThat(deleted).isTrue();

        verify(elasticSearchService).deleteProduct(mocked);
        verify(imageService).deleteImage("origin-url");
        verify(imageService).deleteImage("detail-url");
        verify(imageService).deleteImage("thumb-url");
    }
}
