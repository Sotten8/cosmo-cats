package com.cosmocats.marketplace.domain.repository;

import java.util.UUID;

public interface CategoryRepository {

    boolean existsById(UUID id);
}
