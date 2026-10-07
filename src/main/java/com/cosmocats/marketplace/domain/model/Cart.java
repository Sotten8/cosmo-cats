package com.cosmocats.marketplace.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record Cart(UUID id, UUID customerId, List<CartItem> items) {

    public Cart {
        items = List.copyOf(items);
    }

    public Cart addItem(UUID productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        List<CartItem> updated = new ArrayList<>(items);
        for (int i = 0; i < updated.size(); i++) {
            CartItem existing = updated.get(i);
            if (existing.productId().equals(productId)) {
                updated.set(i, new CartItem(productId, existing.quantity() + quantity));
                return new Cart(id, customerId, updated);
            }
        }
        updated.add(new CartItem(productId, quantity));
        return new Cart(id, customerId, updated);
    }
}
