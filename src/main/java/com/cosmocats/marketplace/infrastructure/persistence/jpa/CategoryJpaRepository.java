package com.cosmocats.marketplace.infrastructure.persistence.jpa;

import com.cosmocats.marketplace.infrastructure.persistence.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CategoryJpaRepository extends JpaRepository<CategoryEntity, Long> {

    Optional<CategoryEntity> findByPublicId(UUID publicId);

    boolean existsByPublicId(UUID publicId);

    Optional<CategoryEntity> findFirstByNameIgnoreCase(String name);
}
