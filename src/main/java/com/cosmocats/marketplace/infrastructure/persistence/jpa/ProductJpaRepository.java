package com.cosmocats.marketplace.infrastructure.persistence.jpa;

import com.cosmocats.marketplace.infrastructure.persistence.entity.ProductEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductJpaRepository extends JpaRepository<ProductEntity, Long> {

    @Override
    @EntityGraph(attributePaths = "category")
    Page<ProductEntity> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Optional<ProductEntity> findByPublicId(UUID publicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ProductEntity> findWithLockByPublicId(UUID publicId);

    @EntityGraph(attributePaths = "category")
    Optional<ProductEntity> findFirstByCategoryPublicIdAndProductNameIgnoreCase(UUID categoryPublicId, String productName);

    List<ProductSummaryProjection> findByCategoryPublicIdAndPriceLessThanEqualOrderByPriceAsc(
            UUID categoryPublicId, BigDecimal maxPrice);
}
