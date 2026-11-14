package com.cosmocats.marketplace.infrastructure.persistence;

import com.cosmocats.marketplace.domain.exception.DuplicateCategoryNameException;
import com.cosmocats.marketplace.domain.exception.ResourceInUseException;
import com.cosmocats.marketplace.domain.model.Category;
import com.cosmocats.marketplace.support.AbstractPostgresTest;
import com.cosmocats.marketplace.support.DbTestData;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaCategoryRepository.class, JpaProductRepository.class})
class JpaCategoryRepositoryIT extends AbstractPostgresTest {

    @Autowired
    private JpaCategoryRepository categories;
    @Autowired
    private JpaProductRepository products;
    @Autowired
    private EntityManager entityManager;

    @Test
    void savesAndReadsCategory() {
        Category saved = categories.save(new Category(UUID.randomUUID(), "Cosmic Beds"));

        assertThat(categories.findById(saved.id())).contains(saved);
        assertThat(categories.existsById(saved.id())).isTrue();
        assertThat(categories.findByNameIgnoreCase("cosmic BEDS")).contains(saved);
        assertThat(categories.findAll()).contains(saved);
    }

    @Test
    void unknownCategoryIsNotFound() {
        UUID id = UUID.randomUUID();

        assertThat(categories.findById(id)).isEmpty();
        assertThat(categories.existsById(id)).isFalse();
        assertThat(categories.deleteById(id)).isFalse();
    }

    @Test
    void saveWithExistingIdRenamesCategory() {
        Category created = categories.save(new Category(UUID.randomUUID(), "Old Name"));

        categories.save(new Category(created.id(), "New Name"));

        assertThat(categories.findById(created.id())).get().extracting(Category::name).isEqualTo("New Name");
    }

    @Test
    void deleteRemovesCategory() {
        Category created = categories.save(new Category(UUID.randomUUID(), "Temporary"));

        assertThat(categories.deleteById(created.id())).isTrue();
        assertThat(categories.findById(created.id())).isEmpty();
    }

    @Test
    void duplicateNameViolatesUniqueConstraint() {
        categories.save(new Category(UUID.randomUUID(), "Unique Name"));

        assertThatThrownBy(() -> categories.save(new Category(UUID.randomUUID(), "Unique Name")))
                .isInstanceOf(DuplicateCategoryNameException.class);
    }

    @Test
    void categoryWithProductsCannotBeDeleted() {
        Category category = DbTestData.category(categories, "Busy");
        DbTestData.product(products, category.id(), "Busy Product", "5.00", 1);
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> categories.deleteById(category.id()))
                .isInstanceOf(ResourceInUseException.class);
    }
}
