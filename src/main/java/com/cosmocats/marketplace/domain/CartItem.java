package com.cosmocats.marketplace.domain;

import java.util.UUID;

public record CartItem(UUID productId, int quantity) {
}
