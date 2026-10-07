package com.cosmocats.marketplace.domain.exception;

import java.util.UUID;

public class CategoryNotFoundException extends RuntimeException {

    private final UUID categoryId;

    public CategoryNotFoundException(UUID categoryId) {
        super("Category with id '" + categoryId + "' does not exist.");
        this.categoryId = categoryId;
    }

    public UUID categoryId() {
        return categoryId;
    }
}
