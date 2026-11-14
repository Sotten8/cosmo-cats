package com.cosmocats.marketplace.application.product;

import com.cosmocats.marketplace.domain.common.PageResult;
import com.cosmocats.marketplace.domain.exception.CategoryNotFoundException;
import com.cosmocats.marketplace.domain.exception.DuplicateProductNameException;
import com.cosmocats.marketplace.domain.exception.ProductNotFoundException;
import com.cosmocats.marketplace.domain.model.Product;
import com.cosmocats.marketplace.domain.repository.CategoryRepository;
import com.cosmocats.marketplace.domain.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class ProductService {

    private final ProductRepository products;
    private final CategoryRepository categories;

    public ProductService(ProductRepository products, CategoryRepository categories) {
        this.products = products;
        this.categories = categories;
    }

    @Transactional(readOnly = true)
    public PageResult<Product> findAll(int page, int size) {
        return products.findAll(page, size);
    }

    @Transactional(readOnly = true)
    public Product findById(UUID id) {
        return products.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    public Product create(Product product) {
        ensureCategoryExists(product.categoryId());
        ensureNameIsFree(product.categoryId(), product.name(), product.id());
        return products.save(product);
    }

    public Product update(UUID id, Product incoming) {
        Product existing = findById(id);
        ensureCategoryExists(incoming.categoryId());
        ensureNameIsFree(incoming.categoryId(), incoming.name(), id);
        Product updated = new Product(
                id,
                incoming.name(),
                incoming.description(),
                incoming.price(),
                incoming.stock(),
                incoming.categoryId(),
                incoming.sellerEmail(),
                existing.createdAt());
        return products.save(updated);
    }

    public void delete(UUID id) {
        if (!products.deleteById(id)) {
            throw new ProductNotFoundException(id);
        }
    }

    private void ensureCategoryExists(UUID categoryId) {
        if (!categories.existsById(categoryId)) {
            throw new CategoryNotFoundException(categoryId);
        }
    }

    private void ensureNameIsFree(UUID categoryId, String name, UUID ownerId) {
        products.findByCategoryIdAndNameIgnoreCase(categoryId, name)
                .filter(other -> !other.id().equals(ownerId))
                .ifPresent(other -> {
                    throw new DuplicateProductNameException(name);
                });
    }
}
