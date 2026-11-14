package com.cosmocats.marketplace.domain.exception;

public class ResourceInUseException extends RuntimeException {

    public ResourceInUseException(String resource, Object id) {
        super(resource + " '" + id + "' is still referenced by other records and cannot be deleted.");
    }
}
