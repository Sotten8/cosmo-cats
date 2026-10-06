package com.cosmocats.marketplace.service;

import com.cosmocats.marketplace.domain.Product;
import com.cosmocats.marketplace.domain.ProductNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock implementation: data lives in memory until the database is connected.
 */
@Service
public class ProductService {

    private final Map<UUID, Product> store = new ConcurrentHashMap<>();

    public ProductService() {
        seed("Star-Dust Anti-Gravity Yarn Ball", "A ball of yarn that floats. Cats approve.",
                "19.99", 120, 1L, "yarn@cosmo-cats.market");
        seed("Cosmic Milk 1L", "Milk from the Andromeda dairy asteroid.",
                "7.50", 300, 2L, "milk@cosmo-cats.market");
        seed("Comet Tail Catnip", "Catnip harvested from a passing comet.",
                "12.00", 75, 3L, "catnip@cosmo-cats.market");
    }

    public PageResult<Product> findAll(int page, int size) {
        List<Product> sorted = store.values().stream()
                .sorted(Comparator.comparing(Product::createdAt).thenComparing(Product::name))
                .toList();
        int from = (int) Math.min((long) page * size, sorted.size());
        int to = Math.min(from + size, sorted.size());
        return new PageResult<>(sorted.subList(from, to), sorted.size());
    }

    public Product findById(UUID id) {
        Product product = store.get(id);
        if (product == null) {
            throw new ProductNotFoundException(id);
        }
        return product;
    }

    public Product create(Product product) {
        store.put(product.id(), product);
        return product;
    }

    public Product update(UUID id, Product incoming) {
        Product existing = findById(id);
        Product updated = new Product(
                id,
                incoming.name(),
                incoming.description(),
                incoming.price(),
                incoming.stock(),
                incoming.categoryId(),
                incoming.sellerEmail(),
                existing.createdAt());
        store.put(id, updated);
        return updated;
    }

    public void delete(UUID id) {
        if (store.remove(id) == null) {
            throw new ProductNotFoundException(id);
        }
    }

    private void seed(String name, String description, String price, int stock, Long categoryId, String email) {
        Product product = new Product(UUID.randomUUID(), name, description, new BigDecimal(price),
                stock, categoryId, email, Instant.now());
        store.put(product.id(), product);
    }
}
