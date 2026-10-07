package com.cosmocats.marketplace.infrastructure.persistence;

import java.util.UUID;

/** Fixed ids of the demo categories, shared by the in-memory repositories. */
final class SeedData {

    static final UUID TOYS = UUID.fromString("11111111-1111-1111-1111-111111111111");
    static final UUID FOOD = UUID.fromString("22222222-2222-2222-2222-222222222222");
    static final UUID TREATS = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private SeedData() {
    }
}
