package com.cosmocats.marketplace.support;

import com.cosmocats.marketplace.domain.model.Category;
import com.cosmocats.marketplace.domain.model.Money;
import com.cosmocats.marketplace.domain.model.Product;
import com.cosmocats.marketplace.domain.repository.CategoryRepository;
import com.cosmocats.marketplace.domain.repository.ProductRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class DbTestData {

    private DbTestData() {
    }

    public static Category category(CategoryRepository categories, String namePrefix) {
        return categories.save(new Category(UUID.randomUUID(), namePrefix + " " + UUID.randomUUID()));
    }

    public static Product product(ProductRepository products, UUID categoryId, String name, String price, int stock) {
        return products.save(new Product(UUID.randomUUID(), name, "test product",
                new Money(new BigDecimal(price), "GCR"), stock, categoryId, "seller@cosmo-cats.market",
                Instant.now()));
    }
}
