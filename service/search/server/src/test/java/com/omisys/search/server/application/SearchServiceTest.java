package com.omisys.search.server.application;

import com.omisys.search.server.domain.ProductSearchDto;
import com.omisys.search.server.domain.SortOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock private ElasticsearchOperations elasticsearchOperations;

    @InjectMocks private SearchService searchService;

    @Mock private SearchHits<ProductSearchDto> searchHits;

    @Test
    @DisplayName("searchProducts: 필터/페이징/정렬이 구성된 NativeQuery로 elasticsearchOperations.search에 위임된다")
    void searchProducts_builds_query_and_delegates() {
        // given
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(ProductSearchDto.class)))
                .thenReturn(searchHits);

        // when
        SearchHits<ProductSearchDto> result = searchService.searchProducts(
                "phone",         // keyword
                10L,             // categoryId
                "APPLE",         // brandName
                "BLACK",         // mainColor
                1000.0,          // minPrice
                5000.0,          // maxPrice
                "M",             // size
                SortOption.PRICE_LOW_TO_HIGH, // sortOption
                0,               // page
                20               // pageSize
        );

        // then
        assertThat(result).isSameAs(searchHits);

        ArgumentCaptor<NativeQuery> captor = ArgumentCaptor.forClass(NativeQuery.class);
        verify(elasticsearchOperations).search(captor.capture(), eq(ProductSearchDto.class));

        NativeQuery q = captor.getValue();
        assertThat(q).isNotNull();

        // (1) pageable 검증: toString이 아니라 실제 값으로 검증
        Pageable pageable = (Pageable) ReflectionTestUtils.getField(q, "pageable");
        assertThat(pageable).isNotNull();
        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(20);

        // (2) keyword wrapping 검증: *keyword*가 들어갔는지
        // query 객체 구조는 ES 라이브러리 버전에 따라 다르므로, 실무에서도 toString 기반 검증이 보편적
        Object queryObj = ReflectionTestUtils.getField(q, "query");
        assertThat(String.valueOf(queryObj)).contains("*phone*");

        // (3) sort 검증: RELEVANCE가 아니므로 "UNSORTED가 아닌 값"이어야 함
        // (NativeQuery 구현에 따라 null이 아니라 UNSORTED로 기본값이 들어갈 수 있음)
        Object sort = ReflectionTestUtils.getField(q, "sort");
        assertThat(sort).isNotNull();
        assertThat(String.valueOf(sort)).doesNotContain("UNSORTED");
    }

    @Test
    @DisplayName("searchProducts: sortOption이 RELEVANCE면 정렬은 기본값(UNSORTED) 상태다")
    void searchProducts_relevance_results_in_unsorted() {
        // given
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(ProductSearchDto.class)))
                .thenReturn(searchHits);

        // when
        searchService.searchProducts(
                "phone",
                null,
                null,
                null,
                null,
                null,
                null,
                SortOption.RELEVANCE,
                1,
                10
        );

        // then
        ArgumentCaptor<NativeQuery> captor = ArgumentCaptor.forClass(NativeQuery.class);
        verify(elasticsearchOperations).search(captor.capture(), eq(ProductSearchDto.class));

        NativeQuery q = captor.getValue();

        Object sort = ReflectionTestUtils.getField(q, "sort");

        // 핵심: 이 프로젝트/라이브러리에서는 "세팅 안 함"이 null이 아니라 UNSORTED로 들어온다.
        assertThat(sort).isNotNull();
        assertThat(String.valueOf(sort)).contains("UNSORTED");
    }
}
