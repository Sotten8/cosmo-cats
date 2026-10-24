package com.cosmocats.marketplace.domain.common;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageResultTest {

    @Test
    void calculatesTotalPagesRoundingUp() {
        assertThat(new PageResult<>(List.of(1, 2), 0, 2, 5).totalPages()).isEqualTo(3);
        assertThat(new PageResult<>(List.of(1, 2), 0, 2, 4).totalPages()).isEqualTo(2);
    }

    @Test
    void emptyResultHasZeroPages() {
        assertThat(new PageResult<Integer>(List.of(), 0, 10, 0).totalPages()).isZero();
    }

    @Test
    void rejectsNegativePage() {
        assertThatThrownBy(() -> new PageResult<Integer>(List.of(), -1, 10, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("page");
    }

    @Test
    void rejectsNonPositiveSize() {
        assertThatThrownBy(() -> new PageResult<Integer>(List.of(), 0, 0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size");
    }

    @Test
    void rejectsNegativeTotalElements() {
        assertThatThrownBy(() -> new PageResult<Integer>(List.of(), 0, 10, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("totalElements");
    }

    @Test
    void itemsAreDefensivelyCopied() {
        List<Integer> source = new ArrayList<>(List.of(1, 2));
        PageResult<Integer> page = new PageResult<>(source, 0, 10, 2);

        source.add(3);

        assertThat(page.items()).containsExactly(1, 2);
        assertThatThrownBy(() -> page.items().add(4)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void mapTransformsItemsAndKeepsPagingInfo() {
        PageResult<Integer> page = new PageResult<>(List.of(1, 2, 3), 2, 3, 9);

        PageResult<String> mapped = page.map(i -> "n" + i);

        assertThat(mapped.items()).containsExactly("n1", "n2", "n3");
        assertThat(mapped.page()).isEqualTo(2);
        assertThat(mapped.size()).isEqualTo(3);
        assertThat(mapped.totalElements()).isEqualTo(9);
    }
}
