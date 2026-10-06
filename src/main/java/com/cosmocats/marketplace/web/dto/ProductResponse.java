package com.cosmocats.marketplace.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
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
