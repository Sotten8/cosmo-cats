package com.cosmocats.marketplace.application.report;

import com.cosmocats.marketplace.domain.model.OrderStatus;
import com.cosmocats.marketplace.domain.model.ProductSales;
import com.cosmocats.marketplace.domain.model.ProductSummary;
import com.cosmocats.marketplace.domain.repository.OrderRepository;
import com.cosmocats.marketplace.domain.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private OrderRepository orders;
    @Mock
    private ProductRepository products;
    @InjectMocks
    private ReportService service;

    @Test
    void topSellingProductsAsksForPurchasedStatusesOnly() {
        List<ProductSales> rows = List.of(new ProductSales(UUID.randomUUID(), "Yarn", 9));
        when(orders.findTopSellingProducts(
                Set.of(OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.DELIVERED), 5)).thenReturn(rows);

        assertThat(service.topSellingProducts(5)).isEqualTo(rows);
    }

    @Test
    void topSellingProductsRejectsNonPositiveLimit() {
        assertThatThrownBy(() -> service.topSellingProducts(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void affordableProductsDelegates() {
        UUID category = UUID.randomUUID();
        List<ProductSummary> rows = List.of(new ProductSummary(UUID.randomUUID(), "Yarn", new BigDecimal("1.00")));
        when(products.findSummariesInCategoryUpToPrice(category, new BigDecimal("5"))).thenReturn(rows);

        assertThat(service.affordableProducts(category, new BigDecimal("5"))).isEqualTo(rows);
    }
}
