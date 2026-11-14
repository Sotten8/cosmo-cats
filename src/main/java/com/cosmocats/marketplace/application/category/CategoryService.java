package com.cosmocats.marketplace.application.category;

import com.cosmocats.marketplace.domain.exception.CategoryNotFoundException;
import com.cosmocats.marketplace.domain.exception.DuplicateCategoryNameException;
import com.cosmocats.marketplace.domain.model.Category;
import com.cosmocats.marketplace.domain.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categories;

    public CategoryService(CategoryRepository categories) {
        this.categories = categories;
    }

    @Transactional(readOnly = true)
    public List<Category> findAll() {
        return categories.findAll();
    }

    @Transactional(readOnly = true)
    public Category findById(UUID id) {
        return categories.findById(id).orElseThrow(() -> new CategoryNotFoundException(id));
    }

    public Category create(String name) {
        Category category = new Category(UUID.randomUUID(), name.strip());
        ensureNameIsFree(category.name(), category.id());
        return categories.save(category);
    }

    public Category rename(UUID id, String newName) {
        Category existing = findById(id);
        Category renamed = new Category(existing.id(), newName.strip());
        ensureNameIsFree(renamed.name(), id);
        return categories.save(renamed);
    }

    public void delete(UUID id) {
        if (!categories.deleteById(id)) {
            throw new CategoryNotFoundException(id);
        }
    }

    private void ensureNameIsFree(String name, UUID ownerId) {
        categories.findByNameIgnoreCase(name)
                .filter(other -> !other.id().equals(ownerId))
                .ifPresent(other -> {
                    throw new DuplicateCategoryNameException(name);
                });
    }
}
