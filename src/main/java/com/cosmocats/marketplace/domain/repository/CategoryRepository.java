package com.cosmocats.marketplace.domain.repository;

import java.util.UUID;

/** Port: read access to the product categories. */
public interface CategoryRepository {

    boolean existsById(UUID id);
}
