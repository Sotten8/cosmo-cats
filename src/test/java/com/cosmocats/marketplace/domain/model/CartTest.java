package com.cosmocats.marketplace.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartTest {

    private final UUID productA = UUID.randomUUID();
    private final UUID productB = UUID.randomUUID();
    private final Cart empty = new Cart(UUID.randomUUID(), UUID.randomUUID(), List.of());

    @Test
    void addsNewItem() {
        Cart cart = empty.addItem(productA, 2);

        assertThat(cart.items()).containsExactly(new CartItem(productA, 2));
    }

    @Test
    void mergesQuantityForSameProduct() {
        Cart cart = empty.addItem(productA, 2).addItem(productB, 1).addItem(productA, 3);

        assertThat(cart.items()).containsExactly(new CartItem(productA, 5), new CartItem(productB, 1));
    }

    @Test
    void isImmutable() {
        empty.addItem(productA, 1);

        assertThat(empty.items()).isEmpty();
    }

    @Test
    void rejectsNonPositiveQuantity() {
        assertThatThrownBy(() -> empty.addItem(productA, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> empty.addItem(productA, -1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cartItemValidatesItself() {
        assertThatThrownBy(() -> new CartItem(null, 1)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CartItem(productA, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}