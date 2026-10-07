package com.cosmocats.marketplace.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Amount of money together with its currency (3-letter uppercase code, e.g. GCR - Galactic Credit).
 * The amount is never negative and is normalised to exactly 2 decimal places.
 */
public record Money(BigDecimal amount, String currency) {

    private static final Pattern CURRENCY_CODE = Pattern.compile("[A-Z]{3}");

    public Money {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("amount must have at most 2 decimal places");
        }
        if (!CURRENCY_CODE.matcher(currency).matches()) {
            throw new IllegalArgumentException("currency must be a 3-letter uppercase code");
        }
        amount = amount.setScale(2, RoundingMode.UNNECESSARY);
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money multiply(int factor) {
        if (factor < 0) {
            throw new IllegalArgumentException("factor must not be negative");
        }
        return new Money(amount.multiply(BigDecimal.valueOf(factor)), currency);
    }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "currency mismatch: " + currency + " vs " + other.currency);
        }
    }
}
