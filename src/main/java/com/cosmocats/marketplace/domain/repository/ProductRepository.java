package com.cosmocats.marketplace.domain.repository;

import com.cosmocats.marketplace.domain.common.PageResult;
import com.cosmocats.marketplace.domain.model.Product;
import com.cosmocats.marketplace.domain.model.ProductSummary;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {

    PageResult<Product> findAll(int page, int size);

    Optional<Product> findById(UUID id);

    Optional<Product> findByIdForUpdate(UUID id);

    Optional<Product> findByCategoryIdAndNameIgnoreCase(UUID categoryId, String name);

    List<ProductSummary> findSummariesInCategoryUpToPrice(UUID categoryId, BigDecimal maxPrice);

    Product save(Product product);

    boolean deleteById(UUID id);
}
