package com.cosmocats.marketplace.infrastructure.persistence.entity;

import com.cosmocats.marketplace.domain.model.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EntityIdentityTest {

    private static void assertIdentityRules(Object first, Object sameRow, Object otherRow, Object unsaved) {
        assertThat(first).isEqualTo(first);
        assertThat(first).isEqualTo(sameRow);
        assertThat(first).isNotEqualTo(otherRow);
        assertThat(first).isNotEqualTo(unsaved);
        assertThat(unsaved).isNotEqualTo(first);
        assertThat(first).isNotEqualTo("not an entity");
        assertThat(first).hasSameHashCodeAs(sameRow);
    }

    private static <T> T withId(T entity, long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    @Test
    void categoryIdentity() {
        assertIdentityRules(
                withId(new CategoryEntity(UUID.randomUUID(), "A"), 1),
                withId(new CategoryEntity(UUID.randomUUID(), "B"), 1),
                withId(new CategoryEntity(UUID.randomUUID(), "C"), 2),
                new CategoryEntity(UUID.randomUUID(), "D"));
    }

    @Test
    void categoryExposesItsFields() {
        UUID publicId = UUID.randomUUID();
        CategoryEntity category = withId(new CategoryEntity(publicId, "Toys"), 5);
        category.setName("Games");

        assertThat(category.getId()).isEqualTo(5);
        assertThat(category.getPublicId()).isEqualTo(publicId);
        assertThat(category.getName()).isEqualTo("Games");
    }

    @Test
    void productIdentityAndAccessors() {
        Instant now = Instant.now();
        assertIdentityRules(
                withId(new ProductEntity(UUID.randomUUID(), now), 1),
                withId(new ProductEntity(UUID.randomUUID(), now), 1),
                withId(new ProductEntity(UUID.randomUUID(), now), 2),
                new ProductEntity(UUID.randomUUID(), now));

        ProductEntity product = withId(new ProductEntity(UUID.randomUUID(), now), 7);
        CategoryEntity category = new CategoryEntity(UUID.randomUUID(), "Toys");
        product.setCategory(category);
        product.setProductName("Yarn");
        product.setDescription("soft");
        product.setPrice(new BigDecimal("1.00"));
        product.setCurrency("GCR");
        product.setStockQuantity(3);
        product.setSellerEmail("a@b.c");

        assertThat(product.getId()).isEqualTo(7);
        assertThat(product.getCategory()).isSameAs(category);
        assertThat(product.getProductName()).isEqualTo("Yarn");
        assertThat(product.getDescription()).isEqualTo("soft");
        assertThat(product.getPrice()).isEqualByComparingTo("1.00");
        assertThat(product.getCurrency()).isEqualTo("GCR");
        assertThat(product.getStockQuantity()).isEqualTo(3);
        assertThat(product.getSellerEmail()).isEqualTo("a@b.c");
        assertThat(product.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void orderIdentityAndItems() {
        Instant now = Instant.now();
        UUID customer = UUID.randomUUID();
        assertIdentityRules(
                withId(new OrderEntity("1", customer, OrderStatus.NEW, now), 1),
                withId(new OrderEntity("2", customer, OrderStatus.NEW, now), 1),
                withId(new OrderEntity("3", customer, OrderStatus.NEW, now), 2),
                new OrderEntity("4", customer, OrderStatus.NEW, now));

        OrderEntity order = withId(new OrderEntity("5", customer, OrderStatus.NEW, now), 9);
        OrderItemEntity item = new OrderItemEntity(
                new ProductEntity(UUID.randomUUID(), now), 2, new BigDecimal("3.00"), "GCR");
        order.addItem(item);
        order.setStatus(OrderStatus.PAID);

        assertThat(order.getId()).isEqualTo(9);
        assertThat(order.getOrderNumber()).isEqualTo("5");
        assertThat(order.getCustomerId()).isEqualTo(customer);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getCreatedAt()).isEqualTo(now);
        assertThat(order.getItems()).containsExactly(item);
        assertThat(item.getQuantity()).isEqualTo(2);
        assertThat(item.getUnitPrice()).isEqualByComparingTo("3.00");
        assertThat(item.getCurrency()).isEqualTo("GCR");
        assertThat(item.getProduct()).isNotNull();
    }

    @Test
    void orderItemIdentity() {
        Instant now = Instant.now();
        ProductEntity product = new ProductEntity(UUID.randomUUID(), now);
        assertIdentityRules(
                withId(new OrderItemEntity(product, 1, BigDecimal.ONE, "GCR"), 1),
                withId(new OrderItemEntity(product, 2, BigDecimal.TEN, "GCR"), 1),
                withId(new OrderItemEntity(product, 1, BigDecimal.ONE, "GCR"), 2),
                new OrderItemEntity(product, 1, BigDecimal.ONE, "GCR"));
    }
}
