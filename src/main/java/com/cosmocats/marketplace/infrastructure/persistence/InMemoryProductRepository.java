package com.cosmocats.marketplace.infrastructure.persistence;

import com.cosmocats.marketplace.domain.common.PageResult;
import com.cosmocats.marketplace.domain.exception.DuplicateProductNameException;
import com.cosmocats.marketplace.domain.model.Money;
import com.cosmocats.marketplace.domain.model.Product;
import com.cosmocats.marketplace.domain.repository.ProductRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryProductRepository implements ProductRepository {

    private static final Comparator<Product> PAGE_ORDER = Comparator
            .comparing(Product::createdAt)
            .thenComparing(Product::name)
            .thenComparing(Product::id);

    private final Map<UUID, Product> store = new ConcurrentHashMap<>();
    private final Object writeLock = new Object();

    public InMemoryProductRepository() {
        seed("Star-Dust Anti-Gravity Yarn Ball", "A ball of yarn that floats. Cats approve.",
                "19.99", 120, SeedData.TOYS, "yarn@cosmo-cats.market");
        seed("Cosmic Milk 1L", "Milk from the Andromeda dairy asteroid.",
                "7.50", 300, SeedData.FOOD, "milk@cosmo-cats.market");
        seed("Comet Tail Catnip", "Catnip harvested from a passing comet.",
                "12.00", 75, SeedData.TREATS, "catnip@cosmo-cats.market");
    }

    @Override
    public PageResult<Product> findAll(int page, int size) {
        List<Product> sorted = store.values().stream().sorted(PAGE_ORDER).toList();
        int from = (int) Math.min((long) page * size, sorted.size());
        int to = Math.min(from + size, sorted.size());
        return new PageResult<>(sorted.subList(from, to), page, size, sorted.size());
    }

    @Override
    public Optional<Product> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<Product> findByNameIgnoreCase(String name) {
        return store.values().stream()
                .filter(product -> product.name().equalsIgnoreCase(name))
                .findFirst();
    }

    @Override
    public Product save(Product product) {
        synchronized (writeLock) {
            boolean nameTaken = store.values().stream()
                    .anyMatch(other -> !other.id().equals(product.id())
                            && other.name().equalsIgnoreCase(product.name()));
            if (nameTaken) {
                throw new DuplicateProductNameException(product.name());
            }
            store.put(product.id(), product);
            return product;
        }
    }

    @Override
    public boolean deleteById(UUID id) {
        return store.remove(id) != null;
    }

    private void seed(String name, String description, String price, int stock, UUID categoryId, String email) {
        Product product = new Product(UUID.randomUUID(), name, description,
                new Money(new BigDecimal(price), "GCR"), stock, categoryId, email, Instant.now());
        store.put(product.id(), product);
    }
}
