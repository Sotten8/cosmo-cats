package com.cosmocats.marketplace.infrastructure.persistence;

import com.cosmocats.marketplace.domain.common.PageResult;
import com.cosmocats.marketplace.domain.exception.CategoryNotFoundException;
import com.cosmocats.marketplace.domain.exception.DuplicateProductNameException;
import com.cosmocats.marketplace.domain.exception.ResourceInUseException;
import com.cosmocats.marketplace.domain.model.Category;
import com.cosmocats.marketplace.domain.model.Money;
import com.cosmocats.marketplace.domain.model.Order;
import com.cosmocats.marketplace.domain.model.OrderItem;
import com.cosmocats.marketplace.domain.model.OrderStatus;
import com.cosmocats.marketplace.domain.model.Product;
import com.cosmocats.marketplace.domain.model.ProductSummary;
import com.cosmocats.marketplace.support.AbstractPostgresTest;
import com.cosmocats.marketplace.support.DbTestData;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaCategoryRepository.class, JpaProductRepository.class, JpaOrderRepository.class})
class JpaProductRepositoryIT extends AbstractPostgresTest {

    @Autowired
    private JpaCategoryRepository categories;
    @Autowired
    private JpaProductRepository products;
    @Autowired
    private JpaOrderRepository orders;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private EntityManager entityManager;

    private Category toys;
    private Category food;

    @BeforeEach
    void createCategories() {
        toys = DbTestData.category(categories, "Toys");
        food = DbTestData.category(categories, "Food");
    }

    @Test
    void createsAndReadsProduct() {
        Product saved = DbTestData.product(products, toys.id(), "Laser Pointer", "9.99", 7);

        Product loaded = products.findById(saved.id()).orElseThrow();

        assertThat(loaded.name()).isEqualTo("Laser Pointer");
        assertThat(loaded.price()).isEqualTo(new Money(new BigDecimal("9.99"), "GCR"));
        assertThat(loaded.stock()).isEqualTo(7);
        assertThat(loaded.categoryId()).isEqualTo(toys.id());
        assertThat(loaded.sellerEmail()).isEqualTo("seller@cosmo-cats.market");
    }

    @Test
    void updatesProductKeepingIdAndCreationTime() {
        Product saved = DbTestData.product(products, toys.id(), "Old Toy", "9.99", 7);
        Product changed = new Product(saved.id(), "New Toy", "better", new Money(new BigDecimal("12.50"), "GCR"),
                3, food.id(), null, Instant.now());

        products.save(changed);

        Product loaded = products.findById(saved.id()).orElseThrow();
        assertThat(loaded.name()).isEqualTo("New Toy");
        assertThat(loaded.categoryId()).isEqualTo(food.id());
        assertThat(loaded.price().amount()).isEqualByComparingTo("12.50");
        assertThat(loaded.sellerEmail()).isNull();
        assertThat(loaded.createdAt()).isEqualTo(saved.createdAt());
    }

    @Test
    void deleteRemovesProduct() {
        Product saved = DbTestData.product(products, toys.id(), "Short Lived", "1.00", 1);

        assertThat(products.deleteById(saved.id())).isTrue();
        assertThat(products.findById(saved.id())).isEmpty();
        assertThat(products.deleteById(saved.id())).isFalse();
    }

    @Test
    void findByIdOfUnknownProductIsEmpty() {
        assertThat(products.findById(UUID.randomUUID())).isEmpty();
        assertThat(products.findByIdForUpdate(UUID.randomUUID())).isEmpty();
    }

    @Test
    void findByIdForUpdateReturnsLockedProduct() {
        Product saved = DbTestData.product(products, toys.id(), "Locked Toy", "2.00", 4);

        assertThat(products.findByIdForUpdate(saved.id())).get().extracting(Product::stock).isEqualTo(4);
    }

    @Test
    void pagesProducts() {
        for (int i = 0; i < 3; i++) {
            DbTestData.product(products, toys.id(), "Paged Toy " + i, "3.00", 1);
        }

        PageResult<Product> first = products.findAll(0, 2);
        PageResult<Product> last = products.findAll(1, 2);

        assertThat(first.items()).hasSize(2);
        assertThat(first.totalElements()).isGreaterThanOrEqualTo(3);
        assertThat(last.page()).isEqualTo(1);
        assertThat(last.items()).isNotEmpty();
    }

    @Test
    void sameNameInSameCategoryViolatesUniqueConstraint() {
        DbTestData.product(products, toys.id(), "Twin", "1.00", 1);

        assertThatThrownBy(() -> DbTestData.product(products, toys.id(), "Twin", "2.00", 1))
                .isInstanceOf(DuplicateProductNameException.class);
    }

    @Test
    void sameNameInAnotherCategoryIsAllowed() {
        DbTestData.product(products, toys.id(), "Twin", "1.00", 1);

        Product other = DbTestData.product(products, food.id(), "Twin", "2.00", 1);

        assertThat(products.findById(other.id())).isPresent();
    }

    @Test
    void findsByCategoryAndNameIgnoringCase() {
        Product saved = DbTestData.product(products, toys.id(), "Mixed Case", "1.00", 1);

        assertThat(products.findByCategoryIdAndNameIgnoreCase(toys.id(), "MIXED case")).contains(saved);
        assertThat(products.findByCategoryIdAndNameIgnoreCase(food.id(), "MIXED case")).isEmpty();
    }

    @Test
    void unknownCategoryIsRejected() {
        assertThatThrownBy(() -> DbTestData.product(products, UUID.randomUUID(), "Orphan", "1.00", 1))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void databaseRejectsNegativePrice() {
        Product saved = DbTestData.product(products, toys.id(), "Check Me", "1.00", 1);

        assertThatThrownBy(() -> jdbc.update("update product set price = -1 where public_id = ?", saved.id()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsNegativeStock() {
        Product saved = DbTestData.product(products, toys.id(), "Check Stock", "1.00", 1);

        assertThatThrownBy(() -> jdbc.update("update product set stock_quantity = -1 where public_id = ?", saved.id()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void projectionReturnsCheapProductsOfCategoryOrderedByPrice() {
        DbTestData.product(products, toys.id(), "Mid", "10.00", 1);
        DbTestData.product(products, toys.id(), "Cheap", "2.50", 1);
        DbTestData.product(products, toys.id(), "Pricey", "99.00", 1);
        DbTestData.product(products, food.id(), "Other Category", "1.00", 1);

        List<ProductSummary> result = products.findSummariesInCategoryUpToPrice(toys.id(), new BigDecimal("10.00"));

        assertThat(result).extracting(ProductSummary::name).containsExactly("Cheap", "Mid");
        assertThat(result).extracting(ProductSummary::price)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactly(new BigDecimal("2.50"), new BigDecimal("10.00"));
    }

    @Test
    void productBoughtInAnOrderCannotBeDeleted() {
        Product saved = DbTestData.product(products, toys.id(), "Sold Toy", "5.00", 10);
        orders.save(new Order(UUID.randomUUID(), null,
                List.of(new OrderItem(saved.id(), saved.name(), saved.price(), 1)),
                OrderStatus.PAID, Instant.now()));
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> products.deleteById(saved.id())).isInstanceOf(ResourceInUseException.class);
    }
}
