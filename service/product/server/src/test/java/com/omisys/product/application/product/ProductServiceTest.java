package com.omisys.product.application.product;

import com.omisys.product.application.dto.ImgDto;
import com.omisys.product.domain.model.Product;
import com.omisys.product.domain.repository.jpa.ProductRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

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
    @DisplayName("createProduct: Product 생성 → 저장 → 응답 반환")
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

    private Product sampleProduct(String name) {
        return Product.builder()
                .categoryId(10L)
                .productName(name)
                .brandName("NIKE")
                .mainColor("BLACK")
                .size("270")
                .originalPrice(BigDecimal.valueOf(1000))
                .discountPercent(null)
                .stock(5)
                .description("d")
                .originImgUrl("o")
                .detailImgUrl("d")
                .thumbnailImgUrl("t")
                .limitCountPerUser(1)
                .tags(List.of("tag"))
                .build();
    }

    @Test
    @DisplayName("getProductList: 리포지토리의 전체 건수를 그대로 유지한다 (페이지 크기로 덮어쓰지 않음)")
    void getProductList_preservesTotalElements() {
        // given — 전체 25건 중 첫 페이지 2건만 조회한 상황
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "salesCount"));
        List<Product> firstPage = List.of(sampleProduct("p1"), sampleProduct("p2"));
        when(productRepository.findAllByFilters(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(firstPage, pageable, 25L));

        // when
        Page<ProductResponse> result =
                productService.getProductList(0, 10, null, null, null, null, null, null, "SALES");

        // then
        assertThat(result.getTotalElements()).isEqualTo(25L);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("getProductList: 필터가 모두 null이면 null 그대로 리포지토리에 전달한다")
    void getProductList_withNullFilters() {
        // given
        when(productRepository.findAllByFilters(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0L));

        // when
        Page<ProductResponse> result =
                productService.getProductList(0, 10, null, null, null, null, null, null, "NEWEST");

        // then — 가격이 null 이어도 BigDecimal 변환에서 NPE 없이 null 이 전달되어야 한다
        verify(productRepository).findAllByFilters(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("getProductList: 필터를 모두 지정하면 값과 정렬 조건이 그대로 전달된다")
    void getProductList_withAllFilters() {
        // given
        when(productRepository.findAllByFilters(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleProduct("p1")), PageRequest.of(1, 5), 6L));

        // when
        Page<ProductResponse> result =
                productService.getProductList(1, 5, 10L, "NIKE", 1_000L, 50_000L, "270", "BLACK", "MIN_PRICE");

        // then
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findAllByFilters(
                eq(10L), eq("NIKE"),
                eq(BigDecimal.valueOf(1_000L)), eq(BigDecimal.valueOf(50_000L)),
                eq("270"), eq("BLACK"), pageableCaptor.capture());

        Pageable captured = pageableCaptor.getValue();
        assertThat(captured.getPageNumber()).isEqualTo(1);
        assertThat(captured.getPageSize()).isEqualTo(5);
        // MIN_PRICE 는 discountedPrice 오름차순
        assertThat(captured.getSort().getOrderFor("discountedPrice")).isNotNull();
        assertThat(captured.getSort().getOrderFor("discountedPrice").getDirection())
                .isEqualTo(Sort.Direction.ASC);

        assertThat(result.getTotalElements()).isEqualTo(6L);
    }
}
