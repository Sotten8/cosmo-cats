package com.cosmocats.marketplace.domain.repository;

import com.cosmocats.marketplace.domain.common.PageResult;
import com.cosmocats.marketplace.domain.exception.DuplicateProductNameException;
import com.cosmocats.marketplace.domain.model.Product;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {

    PageResult<Product> findAll(int page, int size);

    Optional<Product> findById(UUID id);

    Optional<Product> findByNameIgnoreCase(String name);

    Product save(Product product);

    boolean deleteById(UUID id);
}
