package com.cosmocats.marketplace.application.order;

import com.cosmocats.marketplace.domain.exception.InsufficientStockException;
import com.cosmocats.marketplace.domain.exception.OrderNotFoundException;
import com.cosmocats.marketplace.domain.exception.ProductNotFoundException;
import com.cosmocats.marketplace.domain.model.Order;
import com.cosmocats.marketplace.domain.model.OrderStatus;
import com.cosmocats.marketplace.domain.model.Product;
import com.cosmocats.marketplace.domain.repository.OrderRepository;
import com.cosmocats.marketplace.domain.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.cosmocats.marketplace.support.TestData.gcr;
import static com.cosmocats.marketplace.support.TestData.product;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orders;
    @Mock
    private ProductRepository products;
    @InjectMocks
    private OrderService service;

    @Test
    void placeOrderDecreasesStockAndSavesNewOrderWithPriceSnapshot() {
        Product yarn = product();
        when(products.findByIdForUpdate(yarn.id())).thenReturn(Optional.of(yarn));
        when(orders.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order order = service.placeOrder(UUID.randomUUID(), List.of(new OrderLine(yarn.id(), 4)));

        ArgumentCaptor<Product> savedProduct = ArgumentCaptor.forClass(Product.class);
        verify(products).save(savedProduct.capture());
        assertThat(savedProduct.getValue().stock()).isEqualTo(6);
        assertThat(order.status()).isEqualTo(OrderStatus.NEW);
        assertThat(order.items()).singleElement().satisfies(item -> {
            assertThat(item.unitPrice()).isEqualTo(gcr("19.99"));
            assertThat(item.quantity()).isEqualTo(4);
        });
    }

    @Test
    void placeOrderMergesDuplicatedLines() {
        Product yarn = product();
        when(products.findByIdForUpdate(yarn.id())).thenReturn(Optional.of(yarn));
        when(orders.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order order = service.placeOrder(null, List.of(new OrderLine(yarn.id(), 1), new OrderLine(yarn.id(), 2)));

        assertThat(order.items()).singleElement().extracting("quantity").isEqualTo(3);
    }

    @Test
    void placeOrderFailsWhenStockIsNotEnough() {
        Product yarn = product();
        when(products.findByIdForUpdate(yarn.id())).thenReturn(Optional.of(yarn));

        assertThatThrownBy(() -> service.placeOrder(null, List.of(new OrderLine(yarn.id(), 11))))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("requested 11, available 10");
        verify(orders, never()).save(any());
    }

    @Test
    void placeOrderFailsForUnknownProduct() {
        UUID id = UUID.randomUUID();
        when(products.findByIdForUpdate(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.placeOrder(null, List.of(new OrderLine(id, 1))))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void placeOrderRequiresAtLeastOneLine() {
        assertThatThrownBy(() -> service.placeOrder(null, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.placeOrder(null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void orderLineValidatesInput() {
        assertThatThrownBy(() -> new OrderLine(null, 1)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new OrderLine(UUID.randomUUID(), 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void findByIdThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(orders.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id)).isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void findByOrderNumberThrowsWhenMissing() {
        when(orders.findByOrderNumber("x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByOrderNumber("x")).isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void updateStatusKeepsEverythingElse() {
        Product yarn = product();
        Order existing = new Order(UUID.randomUUID(), UUID.randomUUID(),
                List.of(new com.cosmocats.marketplace.domain.model.OrderItem(yarn.id(), yarn.name(), yarn.price(), 1)),
                OrderStatus.NEW, Instant.parse("2026-01-01T00:00:00Z"));
        when(orders.findById(existing.id())).thenReturn(Optional.of(existing));
        when(orders.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order updated = service.updateStatus(existing.id(), OrderStatus.PAID);

        assertThat(updated.status()).isEqualTo(OrderStatus.PAID);
        assertThat(updated.id()).isEqualTo(existing.id());
        assertThat(updated.items()).isEqualTo(existing.items());
    }

    @Test
    void deleteFailsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(orders.deleteById(id)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(OrderNotFoundException.class);
    }
}
