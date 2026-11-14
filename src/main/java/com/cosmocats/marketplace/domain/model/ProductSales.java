package com.cosmocats.marketplace.domain.model;

import java.util.UUID;

public record ProductSales(UUID productId, String productName, long totalQuantity) {
}
