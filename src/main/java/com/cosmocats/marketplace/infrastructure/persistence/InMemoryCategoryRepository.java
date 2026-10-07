package com.cosmocats.marketplace.infrastructure.persistence;

import com.cosmocats.marketplace.domain.model.Category;
import com.cosmocats.marketplace.domain.repository.CategoryRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Mock implementation: a fixed set of categories until the database is connected. */
@Repository
public class InMemoryCategoryRepository implements CategoryRepository {

    private final Map<UUID, Category> store = Stream.of(
                    new Category(SeedData.TOYS, "Toys"),
                    new Category(SeedData.FOOD, "Food and drinks"),
                    new Category(SeedData.TREATS, "Treats"))
            .collect(Collectors.toUnmodifiableMap(Category::id, category -> category));

    @Override
    public boolean existsById(UUID id) {
        return store.containsKey(id);
    }
}
