package com.omisys.product.domain.repository;

import com.omisys.product.infrastructure.utils.ProductSearchDto;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ElasticSearchRepository extends ElasticsearchRepository<ProductSearchDto, String> {
}
