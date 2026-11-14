package com.cosmocats.marketplace.infrastructure.persistence;

import com.cosmocats.marketplace.domain.common.PageResult;
import com.cosmocats.marketplace.domain.exception.ProductNotFoundException;
import com.cosmocats.marketplace.domain.model.Order;
import com.cosmocats.marketplace.domain.model.OrderItem;
import com.cosmocats.marketplace.domain.model.OrderStatus;
import com.cosmocats.marketplace.domain.model.ProductSales;
import com.cosmocats.marketplace.domain.repository.OrderRepository;
import com.cosmocats.marketplace.infrastructure.persistence.entity.OrderEntity;
import com.cosmocats.marketplace.infrastructure.persistence.entity.OrderItemEntity;
import com.cosmocats.marketplace.infrastructure.persistence.jpa.OrderItemJpaRepository;
import com.cosmocats.marketplace.infrastructure.persistence.jpa.OrderJpaRepository;
import com.cosmocats.marketplace.infrastructure.persistence.jpa.ProductJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaOrderRepository implements OrderRepository {

    private static final Sort PAGE_ORDER = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id"));

    private final OrderJpaRepository orders;
    private final OrderItemJpaRepository orderItems;
    private final ProductJpaRepository products;

    @PersistenceContext
    private EntityManager entityManager;

    public JpaOrderRepository(OrderJpaRepository orders, OrderItemJpaRepository orderItems,
                              ProductJpaRepository products) {
        this.orders = orders;
        this.orderItems = orderItems;
        this.products = products;
    }

    @Override
    public PageResult<Order> findAll(int page, int size) {
        Page<OrderEntity> result = orders.findAll(PageRequest.of(page, size, PAGE_ORDER));
        return new PageResult<>(
                result.getContent().stream().map(EntityMapper::toDomain).toList(),
                page, size, result.getTotalElements());
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return findByOrderNumber(id.toString());
    }

    @Override
    public Optional<Order> findByOrderNumber(String orderNumber) {
        return findEntity(orderNumber).map(EntityMapper::toDomain);
    }

    @Override
    public Order save(Order order) {
        OrderEntity entity = findEntity(order.orderNumber()).orElseGet(() -> create(order));
        entity.setStatus(order.status());
        return EntityMapper.toDomain(orders.saveAndFlush(entity));
    }

    @Override
    public boolean deleteById(UUID id) {
        Optional<OrderEntity> entity = findEntity(id.toString());
        entity.ifPresent(orders::delete);
        return entity.isPresent();
    }

    @Override
    public List<ProductSales> findTopSellingProducts(Collection<OrderStatus> statuses, int limit) {
        return orderItems.findTopSellingProducts(statuses, PageRequest.of(0, limit)).stream()
                .map(row -> new ProductSales(row.getProductId(), row.getProductName(), row.getTotalQuantity()))
                .toList();
    }

    private Optional<OrderEntity> findEntity(String orderNumber) {
        return entityManager.unwrap(Session.class)
                .bySimpleNaturalId(OrderEntity.class)
                .loadOptional(orderNumber);
    }

    private OrderEntity create(Order order) {
        OrderEntity entity = new OrderEntity(order.orderNumber(), order.customerId(), order.status(), order.createdAt());
        for (OrderItem item : order.items()) {
            var product = products.findByPublicId(item.productId())
                    .orElseThrow(() -> new ProductNotFoundException(item.productId()));
            entity.addItem(new OrderItemEntity(product, item.quantity(),
                    item.unitPrice().amount(), item.unitPrice().currency()));
        }
        return entity;
    }
}
