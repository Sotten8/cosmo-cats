package com.cosmocats.marketplace.domain.repository;

import com.cosmocats.marketplace.domain.model.Category;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository {

    boolean existsById(UUID id);

    Optional<Category> findById(UUID id);

    Optional<Category> findByNameIgnoreCase(String name);

    List<Category> findAll();

    Category save(Category category);

    boolean deleteById(UUID id);
}
