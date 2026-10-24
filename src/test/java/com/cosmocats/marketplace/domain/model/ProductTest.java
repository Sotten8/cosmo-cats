package com.cosmocats.marketplace.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static com.cosmocats.marketplace.support.TestData.CREATED_AT;
import static com.cosmocats.marketplace.support.TestData.TOYS;
import static com.cosmocats.marketplace.support.TestData.gcr;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    private static Product build(UUID id, String name, String description, Money price, int stock,
                                 UUID categoryId, Instant createdAt) {
        return new Product(id, name, description, price, stock, categoryId, "seller@cosmo-cats.market", createdAt);
    }

    private static void withName(String name) {
        build(UUID.randomUUID(), name, null, gcr("1.00"), 1, TOYS, CREATED_AT);
    }

    private static void withPrice(Money price) {
        build(UUID.randomUUID(), "Star", null, price, 1, TOYS, CREATED_AT);
    }

    private static void withStock(int stock) {
        build(UUID.randomUUID(), "Star", null, gcr("1.00"), stock, TOYS, CREATED_AT);
    }

    @Test
    void createsValidProduct() {
        UUID id = UUID.randomUUID();

        Product product = build(id, "Star Yarn", "desc", gcr("19.99"), 5, TOYS, CREATED_AT);

        assertThat(product.id()).isEqualTo(id);
        assertThat(product.name()).isEqualTo("Star Yarn");
        assertThat(product.price()).isEqualTo(gcr("19.99"));
        assertThat(product.stock()).isEqualTo(5);
    }

    @Test
    void descriptionIsOptional() {
        assertThatCode(() -> build(UUID.randomUUID(), "Star", null, gcr("1.00"), 0, TOYS, CREATED_AT))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void rejectsBlankName(String name) {
        assertThatThrownBy(() -> withName(name))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    void acceptsNameOfMaximumLength() {
        assertThatCode(() -> withName("a".repeat(Product.NAME_MAX_LENGTH))).doesNotThrowAnyException();
    }

    @Test
    void rejectsTooLongName() {
        assertThatThrownBy(() -> withName("a".repeat(Product.NAME_MAX_LENGTH + 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at most");
    }

    @Test
    void rejectsTooLongDescription() {
        String description = "d".repeat(Product.DESCRIPTION_MAX_LENGTH + 1);

        assertThatThrownBy(() -> build(UUID.randomUUID(), "Star", description, gcr("1.00"), 1, TOYS, CREATED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("description");
    }

    @Test
    void rejectsZeroPrice() {
        assertThatThrownBy(() -> withPrice(gcr("0.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than 0");
    }

    @Test
    void acceptsMaximumPrice() {
        assertThatCode(() -> withPrice(new Money(new BigDecimal(Product.MAX_PRICE), "GCR")))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsPriceAboveMaximum() {
        assertThatThrownBy(() -> withPrice(gcr("1000000.01")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("price must be at most");
    }

    @Test
    void rejectsNegativeStock() {
        assertThatThrownBy(() -> withStock(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsStockAboveMaximum() {
        assertThatThrownBy(() -> withStock(Product.MAX_STOCK + 1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptsStockBoundaries() {
        assertThatCode(() -> withStock(0)).doesNotThrowAnyException();
        assertThatCode(() -> withStock(Product.MAX_STOCK)).doesNotThrowAnyException();
    }

    @Test
    void rejectsNullId() {
        assertThatThrownBy(() -> build(null, "Star", null, gcr("1.00"), 1, TOYS, CREATED_AT))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullPrice() {
        assertThatThrownBy(() -> withPrice(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullCategory() {
        assertThatThrownBy(() -> build(UUID.randomUUID(), "Star", null, gcr("1.00"), 1, null, CREATED_AT))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullCreatedAt() {
        assertThatThrownBy(() -> build(UUID.randomUUID(), "Star", null, gcr("1.00"), 1, TOYS, null))
                .isInstanceOf(NullPointerException.class);
    }
}
