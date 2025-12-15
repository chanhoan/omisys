package com.omisys.product.domain.repository.cassandra;

import com.omisys.product.domain.model.Product;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends CassandraRepository<Product, UUID> {

    Optional<Product> findByProductIdAndIsDeletedFalse(UUID productId);

    @Query(
            "SELECT * FROM \"P_PRODUCT\" WHERE "
                    + "(categoryId = ?0) AND "
                    + "(brandName = ?1) AND "
                    + "(originalPrice >= ?2) AND "
                    + "(originalPrice <= ?3) AND "
                    + "(size = ?4) AND "
                    + "(mainColor = ?5) ALLOW FILTERING")
    List<Product> findAllByFilters(
            Long categoryId,
            String brandName,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String productSize,
            String mainColor,
            Pageable pageable);
}
