package com.cosmocats.marketplace.support;

import com.cosmocats.marketplace.domain.model.Money;
import com.cosmocats.marketplace.domain.model.Product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class TestData {

    public static final UUID TOYS = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID FOOD = UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final Instant CREATED_AT = Instant.parse("2026-01-01T10:00:00Z");

    private TestData() {
    }

    public static Money gcr(String amount) {
        return new Money(new BigDecimal(amount), "GCR");
    }

    public static Product product() {
        return product(UUID.randomUUID(), "Star-Dust Yarn Ball");
    }

    public static Product product(UUID id, String name) {
        return new Product(id, name, "A ball of yarn that floats.", gcr("19.99"), 10,
                TOYS, "seller@cosmo-cats.market", CREATED_AT);
    }
}
