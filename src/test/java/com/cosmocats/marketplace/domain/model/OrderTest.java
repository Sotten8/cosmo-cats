package com.cosmocats.marketplace.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.cosmocats.marketplace.support.TestData.gcr;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private final UUID productId = UUID.randomUUID();

    @Test
    void orderItemComputesLineTotal() {
        OrderItem item = new OrderItem(productId, "Star Yarn", gcr("19.99"), 3);

        assertThat(item.lineTotal()).isEqualTo(gcr("59.97"));
    }

    @Test
    void orderItemValidatesInput() {
        assertThatThrownBy(() -> new OrderItem(null, "Star", gcr("1.00"), 1))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new OrderItem(productId, " ", gcr("1.00"), 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OrderItem(productId, "Star", null, 1))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new OrderItem(productId, "Star", gcr("0.00"), 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OrderItem(productId, "Star", gcr("1.00"), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void orderTotalSumsAllLines() {
        Order order = new Order(UUID.randomUUID(), UUID.randomUUID(), List.of(
                new OrderItem(productId, "Star Yarn", gcr("10.00"), 2),
                new OrderItem(UUID.randomUUID(), "Comet Catnip", gcr("2.50"), 4)),
                OrderStatus.NEW, Instant.now());

        assertThat(order.total()).isEqualTo(gcr("30.00"));
    }

    @Test
    void orderMustContainItems() {
        assertThatThrownBy(() -> new Order(UUID.randomUUID(), UUID.randomUUID(), List.of(),
                OrderStatus.NEW, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one item");
    }
}