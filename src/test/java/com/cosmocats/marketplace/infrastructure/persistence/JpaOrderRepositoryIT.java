package com.cosmocats.marketplace.infrastructure.persistence;

import com.cosmocats.marketplace.domain.common.PageResult;
import com.cosmocats.marketplace.domain.exception.ProductNotFoundException;
import com.cosmocats.marketplace.domain.model.Category;
import com.cosmocats.marketplace.domain.model.Money;
import com.cosmocats.marketplace.domain.model.Order;
import com.cosmocats.marketplace.domain.model.OrderItem;
import com.cosmocats.marketplace.domain.model.OrderStatus;
import com.cosmocats.marketplace.domain.model.Product;
import com.cosmocats.marketplace.domain.model.ProductSales;
import com.cosmocats.marketplace.infrastructure.persistence.entity.OrderEntity;
import com.cosmocats.marketplace.infrastructure.persistence.jpa.OrderJpaRepository;
import com.cosmocats.marketplace.support.AbstractPostgresTest;
import com.cosmocats.marketplace.support.DbTestData;
import jakarta.persistence.EntityManager;
import org.hibernate.HibernateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaCategoryRepository.class, JpaProductRepository.class, JpaOrderRepository.class})
class JpaOrderRepositoryIT extends AbstractPostgresTest {

    private static final Set<OrderStatus> PURCHASED =
            Set.of(OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.DELIVERED);

    @Autowired
    private JpaCategoryRepository categories;
    @Autowired
    private JpaProductRepository products;
    @Autowired
    private JpaOrderRepository orders;
    @Autowired
    private OrderJpaRepository orderJpa;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private JdbcTemplate jdbc;

    private Product yarn;
    private Product milk;
    private Product catnip;

    @BeforeEach
    void createProducts() {
        Category category = DbTestData.category(categories, "Report");
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        yarn = DbTestData.product(products, category.id(), "Yarn " + suffix, "10.00", 100);
        milk = DbTestData.product(products, category.id(), "Milk " + suffix, "7.50", 100);
        catnip = DbTestData.product(products, category.id(), "Catnip " + suffix, "12.00", 100);
    }

    private Order order(OrderStatus status, OrderItem... items) {
        return new Order(UUID.randomUUID(), UUID.randomUUID(), List.of(items), status, Instant.now());
    }

    private OrderItem item(Product product, int quantity) {
        return new OrderItem(product.id(), product.name(), product.price(), quantity);
    }

    @Test
    void savesOrderWithItemsAndReadsItBack() {
        Order saved = orders.save(order(OrderStatus.NEW, item(yarn, 2), item(milk, 3)));

        Order loaded = orders.findById(saved.id()).orElseThrow();

        assertThat(loaded.status()).isEqualTo(OrderStatus.NEW);
        assertThat(loaded.items()).extracting(OrderItem::productName)
                .containsExactlyInAnyOrder(yarn.name(), milk.name());
        assertThat(loaded.total()).isEqualTo(new Money(new BigDecimal("42.50"), "GCR"));
        assertThat(loaded.customerId()).isEqualTo(saved.customerId());
    }

    @Test
    void findsOrderByNaturalId() {
        Order saved = orders.save(order(OrderStatus.NEW, item(yarn, 1)));

        assertThat(orders.findByOrderNumber(saved.orderNumber())).get().extracting(Order::id).isEqualTo(saved.id());
        assertThat(orderJpa.findByOrderNumber(saved.orderNumber())).isPresent();
        assertThat(orders.findByOrderNumber(UUID.randomUUID().toString())).isEmpty();
        assertThat(orders.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void orderNumberIsImmutable() {
        Order saved = orders.save(order(OrderStatus.NEW, item(yarn, 1)));
        OrderEntity entity = orderJpa.findByOrderNumber(saved.orderNumber()).orElseThrow();

        ReflectionTestUtils.setField(entity, "orderNumber", UUID.randomUUID().toString());

        assertThatThrownBy(() -> entityManager.flush())
                .isInstanceOf(HibernateException.class)
                .hasMessageContaining("immutable natural identifier");
    }

    @Test
    void saveOfExistingOrderOnlyChangesStatus() {
        Order saved = orders.save(order(OrderStatus.NEW, item(yarn, 1)));

        Order updated = orders.save(new Order(saved.id(), saved.customerId(), saved.items(),
                OrderStatus.SHIPPED, saved.createdAt()));

        assertThat(updated.status()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(updated.items()).hasSize(1);
    }

    @Test
    void deleteRemovesOrderAndItsItems() {
        Order saved = orders.save(order(OrderStatus.NEW, item(yarn, 1)));

        assertThat(orders.deleteById(saved.id())).isTrue();
        entityManager.flush();

        assertThat(orders.findById(saved.id())).isEmpty();
        Integer leftovers = jdbc.queryForObject(
                "select count(*) from order_item where product_id = (select id from product where public_id = ?)",
                Integer.class, yarn.id());
        assertThat(leftovers).isZero();
        assertThat(orders.deleteById(saved.id())).isFalse();
    }

    @Test
    void orderForUnknownProductIsRejected() {
        OrderItem ghost = new OrderItem(UUID.randomUUID(), "Ghost", new Money(new BigDecimal("1.00"), "GCR"), 1);

        assertThatThrownBy(() -> orders.save(order(OrderStatus.NEW, ghost)))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void databaseRejectsDuplicateProductInOneOrder() {
        Order saved = orders.save(order(OrderStatus.NEW, item(yarn, 1)));

        assertThatThrownBy(() -> jdbc.update(
                "insert into order_item (id, order_id, product_id, quantity, unit_price, currency) "
                        + "select nextval('order_item_id_seq'), order_id, product_id, 1, 1, 'GCR' "
                        + "from order_item where order_id = (select id from orders where order_number = ?)",
                saved.orderNumber()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsNonPositiveQuantity() {
        Order saved = orders.save(order(OrderStatus.NEW, item(yarn, 1)));

        assertThatThrownBy(() -> jdbc.update(
                "update order_item set quantity = 0 where order_id = (select id from orders where order_number = ?)",
                saved.orderNumber()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void pagesOrdersNewestFirst() {
        for (int i = 0; i < 3; i++) {
            orders.save(order(OrderStatus.NEW, item(yarn, 1)));
        }

        PageResult<Order> page = orders.findAll(0, 2);

        assertThat(page.items()).hasSize(2);
        assertThat(page.totalElements()).isGreaterThanOrEqualTo(3);
        assertThat(page.items()).allSatisfy(o -> assertThat(o.items()).isNotEmpty());
    }

    @Test
    void topSellingReportRanksProductsByQuantityOfPurchasedOrders() {
        orders.save(order(OrderStatus.PAID, item(yarn, 5), item(milk, 1)));
        orders.save(order(OrderStatus.DELIVERED, item(yarn, 2), item(catnip, 7)));
        orders.save(order(OrderStatus.SHIPPED, item(milk, 3)));
        orders.save(order(OrderStatus.CANCELLED, item(catnip, 100)));
        orders.save(order(OrderStatus.NEW, item(milk, 100)));

        List<ProductSales> report = orders.findTopSellingProducts(PURCHASED, 100).stream()
                .filter(row -> Set.of(yarn.id(), milk.id(), catnip.id()).contains(row.productId()))
                .toList();

        assertThat(report).extracting(ProductSales::productName)
                .containsExactly(catnip.name(), yarn.name(), milk.name());
        assertThat(report).extracting(ProductSales::totalQuantity).containsExactly(7L, 7L, 4L);
    }

    @Test
    void topSellingReportRespectsLimit() {
        orders.save(order(OrderStatus.PAID, item(yarn, 1_000_000), item(milk, 999_999), item(catnip, 5)));

        List<ProductSales> top = orders.findTopSellingProducts(PURCHASED, 2);

        assertThat(top).extracting(ProductSales::productId).containsExactly(yarn.id(), milk.id());
    }
}
