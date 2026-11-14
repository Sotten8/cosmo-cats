package com.cosmocats.marketplace.application.report;

import com.cosmocats.marketplace.domain.model.OrderStatus;
import com.cosmocats.marketplace.domain.model.ProductSales;
import com.cosmocats.marketplace.domain.model.ProductSummary;
import com.cosmocats.marketplace.domain.repository.OrderRepository;
import com.cosmocats.marketplace.domain.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ReportService {

    static final Set<OrderStatus> PURCHASED = Set.of(OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.DELIVERED);

    private final OrderRepository orders;
    private final ProductRepository products;

    public ReportService(OrderRepository orders, ProductRepository products) {
        this.orders = orders;
        this.products = products;
    }

    public List<ProductSales> topSellingProducts(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be positive");
        }
        return orders.findTopSellingProducts(PURCHASED, limit);
    }

    public List<ProductSummary> affordableProducts(UUID categoryId, BigDecimal maxPrice) {
        return products.findSummariesInCategoryUpToPrice(categoryId, maxPrice);
    }
}
