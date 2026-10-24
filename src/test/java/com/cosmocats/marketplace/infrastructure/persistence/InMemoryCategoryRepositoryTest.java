package com.cosmocats.marketplace.infrastructure.persistence;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.cosmocats.marketplace.support.TestData.FOOD;
import static com.cosmocats.marketplace.support.TestData.TOYS;
import static org.assertj.core.api.Assertions.assertThat;

class InMemoryCategoryRepositoryTest {

    private final InMemoryCategoryRepository categories = new InMemoryCategoryRepository();

    @Test
    void knowsSeededCategories() {
        assertThat(categories.existsById(TOYS)).isTrue();
        assertThat(categories.existsById(FOOD)).isTrue();
        assertThat(categories.existsById(UUID.fromString("33333333-3333-3333-3333-333333333333"))).isTrue();
    }

    @Test
    void doesNotKnowRandomCategory() {
        assertThat(categories.existsById(UUID.randomUUID())).isFalse();
    }
}