package com.cosmocats.marketplace.application.order;

import com.cosmocats.marketplace.domain.common.PageResult;
import com.cosmocats.marketplace.domain.exception.InsufficientStockException;
import com.cosmocats.marketplace.domain.exception.OrderNotFoundException;
import com.cosmocats.marketplace.domain.exception.ProductNotFoundException;
import com.cosmocats.marketplace.domain.model.Order;
import com.cosmocats.marketplace.domain.model.OrderItem;
import com.cosmocats.marketplace.domain.model.OrderStatus;
import com.cosmocats.marketplace.domain.model.Product;
import com.cosmocats.marketplace.domain.repository.OrderRepository;
import com.cosmocats.marketplace.domain.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orders;
    private final ProductRepository products;

    public OrderService(OrderRepository orders, ProductRepository products) {
        this.orders = orders;
        this.products = products;
    }

    public Order placeOrder(UUID customerId, List<OrderLine> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("order must contain at least one line");
        }
        Map<UUID, Integer> quantities = new TreeMap<>();
        lines.forEach(line -> quantities.merge(line.productId(), line.quantity(), Integer::sum));

        List<OrderItem> items = new ArrayList<>();
        quantities.forEach((productId, quantity) -> {
            Product product = products.findByIdForUpdate(productId)
                    .orElseThrow(() -> new ProductNotFoundException(productId));
            if (product.stock() < quantity) {
                throw new InsufficientStockException(product.name(), quantity, product.stock());
            }
            products.save(withStock(product, product.stock() - quantity));
            items.add(new OrderItem(product.id(), product.name(), product.price(), quantity));
        });

        return orders.save(new Order(UUID.randomUUID(), customerId, items, OrderStatus.NEW, Instant.now()));
    }

    @Transactional(readOnly = true)
    public PageResult<Order> findAll(int page, int size) {
        return orders.findAll(page, size);
    }

    @Transactional(readOnly = true)
    public Order findById(UUID id) {
        return orders.findById(id).orElseThrow(() -> new OrderNotFoundException(id.toString()));
    }

    @Transactional(readOnly = true)
    public Order findByOrderNumber(String orderNumber) {
        return orders.findByOrderNumber(orderNumber).orElseThrow(() -> new OrderNotFoundException(orderNumber));
    }

    public Order updateStatus(UUID id, OrderStatus status) {
        Order existing = findById(id);
        return orders.save(new Order(existing.id(), existing.customerId(), existing.items(),
                status, existing.createdAt()));
    }

    public void delete(UUID id) {
        if (!orders.deleteById(id)) {
            throw new OrderNotFoundException(id.toString());
        }
    }

    private static Product withStock(Product product, int stock) {
        return new Product(product.id(), product.name(), product.description(), product.price(), stock,
                product.categoryId(), product.sellerEmail(), product.createdAt());
    }
}
