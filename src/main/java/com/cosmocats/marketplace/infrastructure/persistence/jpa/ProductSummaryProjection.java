package com.cosmocats.marketplace.infrastructure.persistence.jpa;

import java.math.BigDecimal;
import java.util.UUID;

public interface ProductSummaryProjection {

    UUID getPublicId();

    String getProductName();

    BigDecimal getPrice();
}
