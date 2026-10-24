package com.cosmocats.marketplace.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void normalizesAmountToTwoDecimalPlaces() {
        Money money = new Money(new BigDecimal("5"), "GCR");

        assertThat(money.amount()).isEqualTo(new BigDecimal("5.00"));
        assertThat(money.amount().scale()).isEqualTo(2);
    }

    @Test
    void acceptsTrailingZerosBeyondTwoDecimals() {
        Money money = new Money(new BigDecimal("2.500"), "GCR");

        assertThat(money.amount()).isEqualTo(new BigDecimal("2.50"));
    }

    @Test
    void rejectsNegativeAmount() {
        assertThatThrownBy(() -> new Money(new BigDecimal("-0.01"), "GCR"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negative");
    }

    @Test
    void rejectsMoreThanTwoDecimalPlaces() {
        assertThatThrownBy(() -> new Money(new BigDecimal("1.234"), "GCR"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2 decimal places");
    }

    @Test
    void rejectsNullAmount() {
        assertThatThrownBy(() -> new Money(null, "GCR")).isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullCurrency() {
        assertThatThrownBy(() -> new Money(BigDecimal.ONE, null)).isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"gcr", "GC", "GCRR", "G1R", ""})
    void rejectsInvalidCurrencyCode(String currency) {
        assertThatThrownBy(() -> new Money(BigDecimal.ONE, currency))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currency");
    }

    @Test
    void isPositiveOnlyForAmountsAboveZero() {
        assertThat(new Money(new BigDecimal("0.01"), "GCR").isPositive()).isTrue();
        assertThat(new Money(BigDecimal.ZERO, "GCR").isPositive()).isFalse();
    }

    @Test
    void addsMoneyOfSameCurrency() {
        Money sum = new Money(new BigDecimal("1.10"), "GCR").add(new Money(new BigDecimal("2.20"), "GCR"));

        assertThat(sum).isEqualTo(new Money(new BigDecimal("3.30"), "GCR"));
    }

    @Test
    void refusesToAddDifferentCurrencies() {
        Money gcr = new Money(BigDecimal.ONE, "GCR");
        Money usd = new Money(BigDecimal.ONE, "USD");

        assertThatThrownBy(() -> gcr.add(usd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currency mismatch");
    }

    @Test
    void multipliesByFactor() {
        Money result = new Money(new BigDecimal("19.99"), "GCR").multiply(3);

        assertThat(result.amount()).isEqualTo(new BigDecimal("59.97"));
    }

    @Test
    void multiplyByZeroGivesZero() {
        assertThat(new Money(new BigDecimal("19.99"), "GCR").multiply(0).amount())
                .isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void refusesNegativeFactor() {
        Money money = new Money(BigDecimal.ONE, "GCR");

        assertThatThrownBy(() -> money.multiply(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
