package com.omisys.product.application.product;

import com.omisys.product.application.dto.ImgDto;
import com.omisys.product.domain.model.Product;
import com.omisys.product.domain.repository.cassandra.ProductRepository;
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

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    @DisplayName("createProduct: Product 생성 → isNew=true 설정 → 저장 → 응답 반환")
    void createProduct_success() {
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
                List.of("shoes", "running")
        );

        ImgDto imgDto = new ImgDto(
                "origin-url",
                "detail-url",
                "thumb-url"
        );

        // save() 호출 시 넘어온 엔티티를 그대로 반환하도록 설정
        when(productRepository.save(any(Product.class)))
                .thenAnswer(inv -> inv.getArgument(0, Product.class));

        // when
        ProductResponse response = productService.createProduct(request, imgDto);

        // then
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository, times(1)).save(captor.capture());

        Product saved = captor.getValue();

        // isNew 플래그는 Cassandra Persistable 처리에 중요 (insert/update 구분)
        assertThat(saved.isNew()).isTrue();

        // 매핑 값 검증
        assertThat(saved.getCategoryId()).isEqualTo(10L);
        assertThat(saved.getProductName()).isEqualTo("AirMax");
        assertThat(saved.getBrandName()).isEqualTo("NIKE");
        assertThat(saved.getOriginImgUrl()).isEqualTo("origin-url");
        assertThat(saved.getDetailImgUrl()).isEqualTo("detail-url");
        assertThat(saved.getThumbnailImgUrl()).isEqualTo("thumb-url");

        // 응답 기본 검증
        assertThat(response.getProductId()).isNotBlank();
        assertThat(response.getProductName()).isEqualTo("AirMax");
        assertThat(response.getDiscountedPrice()).isNotNull();
    }

    @Test
    @DisplayName("getSavedProduct: 미존재(삭제되지 않은 상품 없음) → NOT_FOUND_PRODUCT 예외")
    void getSavedProduct_notFound_throws() {
        // given
        UUID productId = UUID.randomUUID();
        when(productRepository.findByProductIdAndIsDeletedFalse(productId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productService.getSavedProduct(productId))
                .isInstanceOf(ProductException.class)
                .extracting(ex -> ((ProductException) ex).getErrorCode())
                .isEqualTo(ProductErrorCode.NOT_FOUND_PRODUCT);
    }

    @Test
    @DisplayName("updateStatus: soldout 상태 변경 후 저장")
    void updateStatus_success() {
        // given
        UUID productId = UUID.randomUUID();
        Product product = Product.builder()
                .categoryId(1L)
                .productName("p1")
                .brandName("b1")
                .mainColor("c1")
                .size("s1")
                .originalPrice(BigDecimal.valueOf(1000))
                .discountPercent(null)
                .stock(10)
                .description("d")
                .originImgUrl("o")
                .detailImgUrl("d")
                .thumbnailImgUrl("t")
                .limitCountPerUser(1)
                .tags(List.of("tag"))
                .build();

        when(productRepository.findByProductIdAndIsDeletedFalse(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0, Product.class));

        // when
        ProductResponse response = productService.updateStatus(productId, true);

        // then
        verify(productRepository).save(product);
        assertThat(product.isSoldout()).isTrue();
        assertThat(response.isSoldout()).isTrue();
    }

    @Test
    @DisplayName("deleteProduct: isDeleted=true 변경 후 저장")
    void deleteProduct_success() {
        // given
        UUID productId = UUID.randomUUID();
        Product product = Product.builder()
                .categoryId(1L)
                .productName("p1")
                .brandName("b1")
                .mainColor("c1")
                .size("s1")
                .originalPrice(BigDecimal.valueOf(1000))
                .discountPercent(null)
                .stock(10)
                .description("d")
                .originImgUrl("o")
                .detailImgUrl("d")
                .thumbnailImgUrl("t")
                .limitCountPerUser(1)
                .tags(List.of("tag"))
                .build();

        when(productRepository.findByProductIdAndIsDeletedFalse(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0, Product.class));

        // when
        ProductResponse response = productService.deleteProduct(productId);

        // then
        verify(productRepository).save(product);
        assertThat(product.isDeleted()).isTrue();
        assertThat(response.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("reduceStock: 재고 충분 → stock 감소 후 저장")
    void reduceStock_success() {
        // given
        UUID productId = UUID.randomUUID();
        Product product = Product.builder()
                .categoryId(1L)
                .productName("p1")
                .brandName("b1")
                .mainColor("c1")
                .size("s1")
                .originalPrice(BigDecimal.valueOf(1000))
                .discountPercent(null)
                .stock(10)
                .description("d")
                .originImgUrl("o")
                .detailImgUrl("d")
                .thumbnailImgUrl("t")
                .limitCountPerUser(1)
                .tags(List.of("tag"))
                .build();

        when(productRepository.findByProductIdAndIsDeletedFalse(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0, Product.class));

        LinkedHashMap<String, Integer> req = new LinkedHashMap<>();
        req.put(productId.toString(), 3);

        // when
        productService.reduceStock(req);

        // then
        verify(productRepository, times(1)).save(product);
        assertThat(product.getStock()).isEqualTo(7);
    }

    @Test
    @DisplayName("reduceStock: 재고 부족 → STOCK_NOT_AVAILABLE 예외, 저장 호출 없음")
    void reduceStock_insufficient_throws() {
        // given
        UUID productId = UUID.randomUUID();
        Product product = Product.builder()
                .categoryId(1L)
                .productName("p1")
                .brandName("b1")
                .mainColor("c1")
                .size("s1")
                .originalPrice(BigDecimal.valueOf(1000))
                .discountPercent(null)
                .stock(2)
                .description("d")
                .originImgUrl("o")
                .detailImgUrl("d")
                .thumbnailImgUrl("t")
                .limitCountPerUser(1)
                .tags(List.of("tag"))
                .build();

        when(productRepository.findByProductIdAndIsDeletedFalse(productId)).thenReturn(Optional.of(product));

        LinkedHashMap<String, Integer> req = new LinkedHashMap<>();
        req.put(productId.toString(), 3);

        // when & then
        assertThatThrownBy(() -> productService.reduceStock(req))
                .isInstanceOf(ProductException.class)
                .extracting(ex -> ((ProductException) ex).getErrorCode())
                .isEqualTo(ProductErrorCode.STOCK_NOT_AVAILABLE);

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("rollbackStock: stock 복구 후 저장")
    void rollbackStock_success() {
        // given
        UUID productId = UUID.randomUUID();
        Product product = Product.builder()
                .categoryId(1L)
                .productName("p1")
                .brandName("b1")
                .mainColor("c1")
                .size("s1")
                .originalPrice(BigDecimal.valueOf(1000))
                .discountPercent(null)
                .stock(7)
                .description("d")
                .originImgUrl("o")
                .detailImgUrl("d")
                .thumbnailImgUrl("t")
                .limitCountPerUser(1)
                .tags(List.of("tag"))
                .build();

        when(productRepository.findByProductIdAndIsDeletedFalse(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0, Product.class));

        LinkedHashMap<String, Integer> req = new LinkedHashMap<>();
        req.put(productId.toString(), 3);

        // when
        productService.rollbackStock(req);

        // then
        verify(productRepository, times(1)).save(product);
        assertThat(product.getStock()).isEqualTo(10);
    }
}
