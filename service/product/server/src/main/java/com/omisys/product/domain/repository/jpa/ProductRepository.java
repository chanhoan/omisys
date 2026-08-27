package com.omisys.product.domain.repository.jpa;

import com.omisys.product.domain.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findByProductIdAndIsDeletedFalse(UUID productId);

    @Query("""
        SELECT p FROM Product p
        WHERE p.isDeleted = false
          AND (:categoryId IS NULL OR p.categoryId = :categoryId)
          AND (:brandName IS NULL OR p.brandName = :brandName)
          AND (:minPrice IS NULL OR p.originalPrice >= :minPrice)
          AND (:maxPrice IS NULL OR p.originalPrice <= :maxPrice)
          AND (:productSize IS NULL OR p.size = :productSize)
          AND (:mainColor IS NULL OR p.mainColor = :mainColor)
        """)
    Page<Product> findAllByFilters(
            @Param("categoryId") Long categoryId,
            @Param("brandName") String brandName,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("productSize") String productSize,
            @Param("mainColor") String mainColor,
            Pageable pageable);
}
