package com.cosmocats.marketplace.domain;

import java.math.BigDecimal;
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
    }

    public BigDecimal total() {
        return items.stream()
                .map(OrderItem::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
