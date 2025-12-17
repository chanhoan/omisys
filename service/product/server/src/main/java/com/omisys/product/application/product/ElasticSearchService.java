package com.omisys.product.application.product;

import com.omisys.product.domain.model.SortOption;
import com.omisys.product.domain.repository.ElasticSearchRepository;
import com.omisys.product.domain.repository.ElasticsearchCustomRepository;
import com.omisys.product.infrastructure.utils.ProductSearchDto;
import com.omisys.product.presentation.response.ProductResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "ElasticsearchService")
public class ElasticSearchService {

    private final ElasticSearchRepository elasticSearchRepository;
    private final ElasticsearchCustomRepository elasticsearchCustomRepository;

    @Async
    public void saveProduct(ProductResponse response) {
        ProductSearchDto product = ProductSearchDto.toDto(response);
        elasticSearchRepository.save(product);
        log.info("save product in elastic search {}", product.getProductId());
    }

    @Async
    public void updateProduct(ProductResponse response) {
        ProductSearchDto product = ProductSearchDto.toDto(response);
        elasticSearchRepository.save(product);
        log.info("update product in elastic search {}", product.getProductId());
    }

    @Async
    public void deleteProduct(ProductResponse response) {
        ProductSearchDto product = ProductSearchDto.toDto(response);
        elasticSearchRepository.delete(product);
        log.info("delete product in elastic search {}", product.getProductId());
    }

    public Page<ProductSearchDto> getProductList(
            int page,
            int size,
            Long categoryId,
            String brandName,
            Long minPrice,
            Long maxPrice,
            String productSize,
            String mainColor,
            String sortOption) throws IOException {
        SortOption sort = SortOption.valueOf(sortOption.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort.getField()));
        return elasticsearchCustomRepository.searchProductList(
                categoryId,
                brandName,
                minPrice,
                maxPrice,
                productSize,
                mainColor,
                page,
                size,
                pageable,
                sort);
    }
}
