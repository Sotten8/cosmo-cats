package com.cosmocats.marketplace.domain.model;

import java.util.Objects;
import java.util.UUID;

public record CartItem(UUID productId, int quantity) {

    public CartItem {
        Objects.requireNonNull(productId, "productId must not be null");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
    }
}
