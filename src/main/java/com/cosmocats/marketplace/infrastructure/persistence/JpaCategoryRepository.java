package com.cosmocats.marketplace.infrastructure.persistence;

import com.cosmocats.marketplace.domain.exception.DuplicateCategoryNameException;
import com.cosmocats.marketplace.domain.exception.ResourceInUseException;
import com.cosmocats.marketplace.domain.model.Category;
import com.cosmocats.marketplace.domain.repository.CategoryRepository;
import com.cosmocats.marketplace.infrastructure.persistence.entity.CategoryEntity;
import com.cosmocats.marketplace.infrastructure.persistence.jpa.CategoryJpaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaCategoryRepository implements CategoryRepository {

    private final CategoryJpaRepository jpa;

    public JpaCategoryRepository(CategoryJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public boolean existsById(UUID id) {
        return jpa.existsByPublicId(id);
    }

    @Override
    public Optional<Category> findById(UUID id) {
        return jpa.findByPublicId(id).map(EntityMapper::toDomain);
    }

    @Override
    public Optional<Category> findByNameIgnoreCase(String name) {
        return jpa.findFirstByNameIgnoreCase(name).map(EntityMapper::toDomain);
    }

    @Override
    public List<Category> findAll() {
        return jpa.findAll(Sort.by("name")).stream().map(EntityMapper::toDomain).toList();
    }

    @Override
    public Category save(Category category) {
        CategoryEntity entity = jpa.findByPublicId(category.id())
                .orElseGet(() -> new CategoryEntity(category.id(), category.name()));
        entity.setName(category.name());
        try {
            return EntityMapper.toDomain(jpa.saveAndFlush(entity));
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateCategoryNameException(category.name());
        }
    }

    @Override
    public boolean deleteById(UUID id) {
        Optional<CategoryEntity> entity = jpa.findByPublicId(id);
        if (entity.isEmpty()) {
            return false;
        }
        try {
            jpa.delete(entity.get());
            jpa.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ResourceInUseException("Category", id);
        }
        return true;
    }
}
