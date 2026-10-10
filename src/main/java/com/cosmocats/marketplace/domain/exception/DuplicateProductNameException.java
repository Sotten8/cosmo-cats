package com.cosmocats.marketplace.domain.exception;

public class DuplicateProductNameException extends RuntimeException {

    public DuplicateProductNameException(String name) {
        super("A product named '" + name + "' already exists.");
    }
}
