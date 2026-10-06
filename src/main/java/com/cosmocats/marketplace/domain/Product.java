package com.cosmocats.marketplace.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Product aggregate. Immutable; the price is BigDecimal (never double for money).
 * Other aggregates (Category) are referenced by id, as DDD recommends.
 */
public record Product(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        int stock,
        Long categoryId,
        String sellerEmail,
        Instant createdAt
) {
}
