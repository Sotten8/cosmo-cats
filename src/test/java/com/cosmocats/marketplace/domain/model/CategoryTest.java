package com.cosmocats.marketplace.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CategoryTest {

    @Test
    void createsCategory() {
        assertThat(new Category(UUID.randomUUID(), "Toys").name()).isEqualTo("Toys");
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> new Category(UUID.randomUUID(), " "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Category(UUID.randomUUID(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullId() {
        assertThatThrownBy(() -> new Category(null, "Toys")).isInstanceOf(NullPointerException.class);
    }
}