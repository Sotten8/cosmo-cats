package com.cosmocats.marketplace.domain.model;

import java.util.Objects;
import java.util.UUID;

public record OrderItem(UUID productId, String productName, Money unitPrice, int quantity) {

    public OrderItem {
        Objects.requireNonNull(productId, "productId must not be null");
        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("productName must not be blank");
        }
        Objects.requireNonNull(unitPrice, "unitPrice must not be null");
        if (!unitPrice.isPositive()) {
            throw new IllegalArgumentException("unitPrice must be greater than 0");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
    }

    public Money lineTotal() {
        return unitPrice.multiply(quantity);
    }
}
