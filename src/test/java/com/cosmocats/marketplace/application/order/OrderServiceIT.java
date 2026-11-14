package com.cosmocats.marketplace.application.order;

import com.cosmocats.marketplace.application.report.ReportService;
import com.cosmocats.marketplace.domain.exception.InsufficientStockException;
import com.cosmocats.marketplace.domain.exception.OrderNotFoundException;
import com.cosmocats.marketplace.domain.exception.ProductNotFoundException;
import com.cosmocats.marketplace.domain.model.Category;
import com.cosmocats.marketplace.domain.model.Order;
import com.cosmocats.marketplace.domain.model.OrderStatus;
import com.cosmocats.marketplace.domain.model.Product;
import com.cosmocats.marketplace.domain.model.ProductSales;
import com.cosmocats.marketplace.domain.repository.CategoryRepository;
import com.cosmocats.marketplace.domain.repository.ProductRepository;
import com.cosmocats.marketplace.support.AbstractPostgresTest;
import com.cosmocats.marketplace.support.DbTestData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class OrderServiceIT extends AbstractPostgresTest {

    @Autowired
    private OrderService orderService;
    @Autowired
    private ReportService reportService;
    @Autowired
    private CategoryRepository categories;
    @Autowired
    private ProductRepository products;
    @Autowired
    private JdbcTemplate jdbc;

    private static final UUID LAST_UUID = new UUID(Long.MAX_VALUE, Long.MAX_VALUE);

    private Category category;
    private Product yarn;
    private Product milk;

    @BeforeEach
    void setUp() {
        cleanOrders();
        category = DbTestData.category(categories, "OrderServiceIT");
        yarn = DbTestData.product(products, category.id(), "Yarn", "10.00", 5);
        milk = DbTestData.product(products, category.id(), "Milk", "7.50", 2);
    }

    @AfterEach
    void tearDown() {
        cleanOrders();
        jdbc.update("delete from product where category_id = (select id from category where public_id = ?)",
                category.id());
        jdbc.update("delete from category where public_id = ?", category.id());
    }

    private void cleanOrders() {
        jdbc.update("delete from order_item");
        jdbc.update("delete from orders");
    }

    @Test
    void placeOrderStoresOrderAndDecreasesStock() {
        Order order = orderService.placeOrder(UUID.randomUUID(),
                List.of(new OrderLine(yarn.id(), 2), new OrderLine(milk.id(), 1)));

        assertThat(order.status()).isEqualTo(OrderStatus.NEW);
        assertThat(order.total().amount()).isEqualByComparingTo("27.50");
        assertThat(products.findById(yarn.id())).get().extracting(Product::stock).isEqualTo(3);
        assertThat(products.findById(milk.id())).get().extracting(Product::stock).isEqualTo(1);
        assertThat(orderService.findByOrderNumber(order.orderNumber()).id()).isEqualTo(order.id());
        assertThat(orderService.findById(order.id()).items()).hasSize(2);
    }

    @Test
    void duplicatedProductLinesAreMerged() {
        Order order = orderService.placeOrder(UUID.randomUUID(),
                List.of(new OrderLine(yarn.id(), 1), new OrderLine(yarn.id(), 2)));

        assertThat(order.items()).hasSize(1);
        assertThat(order.items().get(0).quantity()).isEqualTo(3);
        assertThat(products.findById(yarn.id())).get().extracting(Product::stock).isEqualTo(2);
    }

    @Test
    void notEnoughStockRollsBackTheWholeOrder() {
        List<Product> byId = Stream.of(yarn, milk).sorted(Comparator.comparing(Product::id)).toList();
        List<OrderLine> lines = List.of(new OrderLine(byId.get(0).id(), 1), new OrderLine(byId.get(1).id(), 999));

        assertThatThrownBy(() -> orderService.placeOrder(UUID.randomUUID(), lines))
                .isInstanceOf(InsufficientStockException.class);

        assertThat(products.findById(yarn.id())).get().extracting(Product::stock).isEqualTo(5);
        assertThat(products.findById(milk.id())).get().extracting(Product::stock).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from orders", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from order_item", Integer.class)).isZero();
    }

    @Test
    void unknownProductRollsBackTheWholeOrder() {
        List<OrderLine> lines = List.of(new OrderLine(yarn.id(), 1), new OrderLine(LAST_UUID, 1));

        assertThatThrownBy(() -> orderService.placeOrder(UUID.randomUUID(), lines))
                .isInstanceOf(ProductNotFoundException.class);

        assertThat(products.findById(yarn.id())).get().extracting(Product::stock).isEqualTo(5);
        assertThat(jdbc.queryForObject("select count(*) from orders", Integer.class)).isZero();
    }

    @Test
    void orderCrudThroughService() {
        Order order = orderService.placeOrder(UUID.randomUUID(), List.of(new OrderLine(yarn.id(), 1)));

        Order paid = orderService.updateStatus(order.id(), OrderStatus.PAID);

        assertThat(paid.status()).isEqualTo(OrderStatus.PAID);
        assertThat(orderService.findAll(0, 10).items()).extracting(Order::id).contains(order.id());

        orderService.delete(order.id());

        assertThatThrownBy(() -> orderService.findById(order.id())).isInstanceOf(OrderNotFoundException.class);
        assertThatThrownBy(() -> orderService.delete(order.id())).isInstanceOf(OrderNotFoundException.class);
        assertThatThrownBy(() -> orderService.findByOrderNumber(order.orderNumber()))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void reportCountsOnlyPurchasedOrders() {
        Order paid = orderService.placeOrder(UUID.randomUUID(), List.of(new OrderLine(yarn.id(), 3)));
        orderService.updateStatus(paid.id(), OrderStatus.PAID);
        Order cancelled = orderService.placeOrder(UUID.randomUUID(), List.of(new OrderLine(milk.id(), 2)));
        orderService.updateStatus(cancelled.id(), OrderStatus.CANCELLED);

        List<ProductSales> report = reportService.topSellingProducts(10);

        assertThat(report).extracting(ProductSales::productId).containsExactly(yarn.id());
        assertThat(report.get(0).totalQuantity()).isEqualTo(3);
    }
}
