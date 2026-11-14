package com.cosmocats.marketplace.domain.exception;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String identifier) {
        super("Order '" + identifier + "' was not found.");
    }
}
