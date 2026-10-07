package com.cosmocats.marketplace.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Order(
        UUID id,
        UUID customerId,
        List<OrderItem> items,
        OrderStatus status,
        Instant createdAt
) {
    public Order {
        items = List.copyOf(items);
        if (items.isEmpty()) {
            throw new IllegalArgumentException("order must contain at least one item");
        }
    }

    /** Sum of all lines; all items must be priced in the same currency. */
    public Money total() {
        return items.stream()
                .map(OrderItem::lineTotal)
                .reduce(Money::add)
                .orElseThrow();
    }
}
