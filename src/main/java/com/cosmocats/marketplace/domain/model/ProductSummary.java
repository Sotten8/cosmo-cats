package com.cosmocats.marketplace.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductSummary(UUID id, String name, BigDecimal price) {
}
