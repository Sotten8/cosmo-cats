package com.cosmocats.marketplace.infrastructure.persistence;

import com.cosmocats.marketplace.domain.common.PageResult;
import com.cosmocats.marketplace.domain.exception.CategoryNotFoundException;
import com.cosmocats.marketplace.domain.exception.DuplicateProductNameException;
import com.cosmocats.marketplace.domain.exception.ResourceInUseException;
import com.cosmocats.marketplace.domain.model.Product;
import com.cosmocats.marketplace.domain.model.ProductSummary;
import com.cosmocats.marketplace.domain.repository.ProductRepository;
import com.cosmocats.marketplace.infrastructure.persistence.entity.CategoryEntity;
import com.cosmocats.marketplace.infrastructure.persistence.entity.ProductEntity;
import com.cosmocats.marketplace.infrastructure.persistence.jpa.CategoryJpaRepository;
import com.cosmocats.marketplace.infrastructure.persistence.jpa.ProductJpaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaProductRepository implements ProductRepository {

    private static final Sort PAGE_ORDER = Sort.by("createdAt", "productName", "publicId");

    private final ProductJpaRepository products;
    private final CategoryJpaRepository categories;

    public JpaProductRepository(ProductJpaRepository products, CategoryJpaRepository categories) {
        this.products = products;
        this.categories = categories;
    }

    @Override
    public PageResult<Product> findAll(int page, int size) {
        Page<ProductEntity> result = products.findAll(PageRequest.of(page, size, PAGE_ORDER));
        return new PageResult<>(
                result.getContent().stream().map(EntityMapper::toDomain).toList(),
                page, size, result.getTotalElements());
    }

    @Override
    public Optional<Product> findById(UUID id) {
        return products.findByPublicId(id).map(EntityMapper::toDomain);
    }

    @Override
    public Optional<Product> findByIdForUpdate(UUID id) {
        return products.findWithLockByPublicId(id).map(EntityMapper::toDomain);
    }

    @Override
    public Optional<Product> findByCategoryIdAndNameIgnoreCase(UUID categoryId, String name) {
        return products.findFirstByCategoryPublicIdAndProductNameIgnoreCase(categoryId, name)
                .map(EntityMapper::toDomain);
    }

    @Override
    public List<ProductSummary> findSummariesInCategoryUpToPrice(UUID categoryId, BigDecimal maxPrice) {
        return products.findByCategoryPublicIdAndPriceLessThanEqualOrderByPriceAsc(categoryId, maxPrice).stream()
                .map(row -> new ProductSummary(row.getPublicId(), row.getProductName(), row.getPrice()))
                .toList();
    }

    @Override
    public Product save(Product product) {
        CategoryEntity category = categories.findByPublicId(product.categoryId())
                .orElseThrow(() -> new CategoryNotFoundException(product.categoryId()));
        ProductEntity entity = products.findByPublicId(product.id())
                .orElseGet(() -> new ProductEntity(product.id(), product.createdAt()));
        entity.setCategory(category);
        entity.setProductName(product.name());
        entity.setDescription(product.description());
        entity.setPrice(product.price().amount());
        entity.setCurrency(product.price().currency());
        entity.setStockQuantity(product.stock());
        entity.setSellerEmail(product.sellerEmail());
        try {
            return EntityMapper.toDomain(products.saveAndFlush(entity));
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateProductNameException(product.name());
        }
    }

    @Override
    public boolean deleteById(UUID id) {
        Optional<ProductEntity> entity = products.findByPublicId(id);
        if (entity.isEmpty()) {
            return false;
        }
        try {
            products.delete(entity.get());
            products.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ResourceInUseException("Product", id);
        }
        return true;
    }
}
