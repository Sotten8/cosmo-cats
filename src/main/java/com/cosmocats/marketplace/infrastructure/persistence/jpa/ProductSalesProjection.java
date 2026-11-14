package com.cosmocats.marketplace.infrastructure.persistence.jpa;

import java.util.UUID;

public interface ProductSalesProjection {

    UUID getProductId();

    String getProductName();

    Long getTotalQuantity();
}
