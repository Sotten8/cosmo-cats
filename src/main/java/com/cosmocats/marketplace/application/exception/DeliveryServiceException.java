package com.cosmocats.marketplace.application.exception;

/** The delivery provider failed (5xx, bad response, connection error). */
public class DeliveryServiceException extends RuntimeException {

    public DeliveryServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
