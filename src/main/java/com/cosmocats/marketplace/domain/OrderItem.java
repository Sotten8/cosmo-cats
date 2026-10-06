package com.cosmocats.marketplace.domain;

import java.math.BigDecimal;
import java.util.UUID;

/** Snapshot of a product at the moment of ordering (name and price must not change later). */
public record OrderItem(UUID productId, String productName, BigDecimal unitPrice, int quantity) {

    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
