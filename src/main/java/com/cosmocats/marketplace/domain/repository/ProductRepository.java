package com.cosmocats.marketplace.domain.repository;

import com.cosmocats.marketplace.domain.common.PageResult;
import com.cosmocats.marketplace.domain.exception.DuplicateProductNameException;
import com.cosmocats.marketplace.domain.model.Product;

import java.util.Optional;
import java.util.UUID;

/** Port: how the domain/application layers store products. Implemented in the infrastructure layer. */
public interface ProductRepository {

    /**
     * Returns the requested page in a stable order: creation time, then name, then id
     * (the id makes the order total, so pages never overlap or lose items).
     */
    PageResult<Product> findAll(int page, int size);

    Optional<Product> findById(UUID id);

    Optional<Product> findByNameIgnoreCase(String name);

    /**
     * Inserts or replaces the product.
     *
     * @throws DuplicateProductNameException if another product already has this name (case-insensitive);
     *                                       the same guarantee a unique index gives in a real database
     */
    Product save(Product product);

    /** @return {@code true} if a product was removed */
    boolean deleteById(UUID id);
}
