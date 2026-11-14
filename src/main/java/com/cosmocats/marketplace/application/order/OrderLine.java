package com.cosmocats.marketplace.application.order;

import java.util.Objects;
import java.util.UUID;

public record OrderLine(UUID productId, int quantity) {

    public OrderLine {
        Objects.requireNonNull(productId, "productId must not be null");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
    }
}
