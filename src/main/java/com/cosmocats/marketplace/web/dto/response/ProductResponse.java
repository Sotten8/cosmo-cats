package com.cosmocats.marketplace.web.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        String currency,
        int stock,
        UUID categoryId,
        Instant createdAt
) {
}
