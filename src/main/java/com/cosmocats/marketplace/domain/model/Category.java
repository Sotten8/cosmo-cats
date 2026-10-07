package com.cosmocats.marketplace.domain.model;

import java.util.Objects;
import java.util.UUID;

public record Category(UUID id, String name) {

    public Category {
        Objects.requireNonNull(id, "id must not be null");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }
}
