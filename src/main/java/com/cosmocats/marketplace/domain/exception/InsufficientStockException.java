package com.cosmocats.marketplace.domain.exception;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String productName, int requested, int available) {
        super("Not enough '" + productName + "' in stock: requested " + requested + ", available " + available + ".");
    }
}
