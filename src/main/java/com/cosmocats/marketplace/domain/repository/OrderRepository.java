package com.cosmocats.marketplace.domain.repository;

import com.cosmocats.marketplace.domain.common.PageResult;
import com.cosmocats.marketplace.domain.model.Order;
import com.cosmocats.marketplace.domain.model.OrderStatus;
import com.cosmocats.marketplace.domain.model.ProductSales;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {

    PageResult<Order> findAll(int page, int size);

    Optional<Order> findById(UUID id);

    Optional<Order> findByOrderNumber(String orderNumber);

    Order save(Order order);

    boolean deleteById(UUID id);

    List<ProductSales> findTopSellingProducts(Collection<OrderStatus> statuses, int limit);
}
