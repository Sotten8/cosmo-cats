package com.cosmocats.marketplace.infrastructure.persistence;

import com.cosmocats.marketplace.domain.model.Category;
import com.cosmocats.marketplace.domain.model.Money;
import com.cosmocats.marketplace.domain.model.Order;
import com.cosmocats.marketplace.domain.model.OrderItem;
import com.cosmocats.marketplace.domain.model.Product;
import com.cosmocats.marketplace.infrastructure.persistence.entity.CategoryEntity;
import com.cosmocats.marketplace.infrastructure.persistence.entity.OrderEntity;
import com.cosmocats.marketplace.infrastructure.persistence.entity.OrderItemEntity;
import com.cosmocats.marketplace.infrastructure.persistence.entity.ProductEntity;

import java.util.UUID;

final class EntityMapper {

    private EntityMapper() {
    }

    static Category toDomain(CategoryEntity entity) {
        return new Category(entity.getPublicId(), entity.getName());
    }

    static Product toDomain(ProductEntity entity) {
        return new Product(
                entity.getPublicId(),
                entity.getProductName(),
                entity.getDescription(),
                new Money(entity.getPrice(), entity.getCurrency()),
                entity.getStockQuantity(),
                entity.getCategory().getPublicId(),
                entity.getSellerEmail(),
                entity.getCreatedAt());
    }

    static Order toDomain(OrderEntity entity) {
        return new Order(
                UUID.fromString(entity.getOrderNumber()),
                entity.getCustomerId(),
                entity.getItems().stream().map(EntityMapper::toDomain).toList(),
                entity.getStatus(),
                entity.getCreatedAt());
    }

    private static OrderItem toDomain(OrderItemEntity entity) {
        ProductEntity product = entity.getProduct();
        return new OrderItem(
                product.getPublicId(),
                product.getProductName(),
                new Money(entity.getUnitPrice(), entity.getCurrency()),
                entity.getQuantity());
    }
}
