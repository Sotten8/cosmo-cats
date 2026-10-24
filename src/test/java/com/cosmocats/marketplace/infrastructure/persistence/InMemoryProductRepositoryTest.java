package com.cosmocats.marketplace.infrastructure.persistence;

import com.cosmocats.marketplace.domain.common.PageResult;
import com.cosmocats.marketplace.domain.exception.DuplicateProductNameException;
import com.cosmocats.marketplace.domain.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.cosmocats.marketplace.support.TestData.FOOD;
import static com.cosmocats.marketplace.support.TestData.product;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryProductRepositoryTest {

    private InMemoryProductRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryProductRepository();
    }

    @Test
    void startsWithThreeSeededProducts() {
        assertThat(repository.findAll(0, 10).totalElements()).isEqualTo(3);
    }

    @Test
    void paginatesResults() {
        PageResult<Product> first = repository.findAll(0, 2);
        PageResult<Product> second = repository.findAll(1, 2);

        assertThat(first.items()).hasSize(2);
        assertThat(second.items()).hasSize(1);
        assertThat(first.totalElements()).isEqualTo(3);
        assertThat(first.totalPages()).isEqualTo(2);
        assertThat(first.items()).doesNotContainAnyElementsOf(second.items());
    }

    @Test
    void pageBeyondTheEndIsEmptyButKeepsTotals() {
        PageResult<Product> page = repository.findAll(50, 10);

        assertThat(page.items()).isEmpty();
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.page()).isEqualTo(50);
    }

    @Test
    void pagingOrderIsStableBetweenCalls() {
        assertThat(repository.findAll(0, 3).items()).isEqualTo(repository.findAll(0, 3).items());
    }

    @Test
    void savesAndFindsProductById() {
        Product saved = repository.save(product());

        assertThat(repository.findById(saved.id())).contains(saved);
        assertThat(repository.findAll(0, 10).totalElements()).isEqualTo(4);
    }

    @Test
    void findByIdReturnsEmptyForUnknownId() {
        assertThat(repository.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void findsByNameIgnoringCase() {
        assertThat(repository.findByNameIgnoreCase("cOsMiC mIlK 1l")).isPresent();
        assertThat(repository.findByNameIgnoreCase("Nothing here")).isEmpty();
    }

    @Test
    void savingExistingIdReplacesProduct() {
        Product original = repository.save(product());
        Product changed = new Product(original.id(), original.name(), "changed", original.price(),
                original.stock(), FOOD, original.sellerEmail(), original.createdAt());

        repository.save(changed);

        assertThat(repository.findById(original.id())).contains(changed);
        assertThat(repository.findAll(0, 10).totalElements()).isEqualTo(4);
    }

    @Test
    void rejectsDuplicateNameOfAnotherProductCaseInsensitively() {
        Product duplicate = product(UUID.randomUUID(), "COSMIC MILK 1L");

        assertThatThrownBy(() -> repository.save(duplicate)).isInstanceOf(DuplicateProductNameException.class);
    }

    @Test
    void deleteRemovesProduct() {
        Product saved = repository.save(product());

        assertThat(repository.deleteById(saved.id())).isTrue();
        assertThat(repository.findById(saved.id())).isEmpty();
    }

    @Test
    void deleteReturnsFalseForUnknownId() {
        assertThat(repository.deleteById(UUID.randomUUID())).isFalse();
    }
}