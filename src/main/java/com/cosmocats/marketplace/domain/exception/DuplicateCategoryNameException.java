package com.cosmocats.marketplace.domain.exception;

public class DuplicateCategoryNameException extends RuntimeException {

    public DuplicateCategoryNameException(String name) {
        super("A category named '" + name + "' already exists.");
    }
}
