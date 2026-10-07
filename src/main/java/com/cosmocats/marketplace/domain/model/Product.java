package com.cosmocats.marketplace.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Product(
        UUID id,
        String name,
        String description,
        Money price,
        int stock,
        UUID categoryId,
        String sellerEmail,
        Instant createdAt
) {

    public static final int NAME_MAX_LENGTH = 100;
    public static final int DESCRIPTION_MAX_LENGTH = 1000;
    public static final String MAX_PRICE = "1000000.00";
    public static final int MAX_STOCK = 100_000;

    private static final BigDecimal MAX_PRICE_VALUE = new BigDecimal(MAX_PRICE);

    public Product {
        Objects.requireNonNull(id, "id must not be null");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (name.strip().length() > NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("name must be at most " + NAME_MAX_LENGTH + " characters");
        }
        if (description != null && description.length() > DESCRIPTION_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "description must be at most " + DESCRIPTION_MAX_LENGTH + " characters");
        }
        Objects.requireNonNull(price, "price must not be null");
        if (!price.isPositive()) {
            throw new IllegalArgumentException("price must be greater than 0");
        }
        if (price.amount().compareTo(MAX_PRICE_VALUE) > 0) {
            throw new IllegalArgumentException("price must be at most " + MAX_PRICE);
        }
        if (stock < 0 || stock > MAX_STOCK) {
            throw new IllegalArgumentException("stock must be between 0 and " + MAX_STOCK);
        }
        Objects.requireNonNull(categoryId, "categoryId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
